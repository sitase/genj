/**
 * GenJ - GenealogyJ
 *
 * Copyright (C) 1997 - 2002 Nils Meier <nils@meiers.net>
 *
 * This piece of code is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package genj.tree;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import genj.gedcom.Change;
import genj.gedcom.Entity;
import genj.gedcom.Fam;
import genj.gedcom.Gedcom;
import genj.gedcom.GedcomListener;
import genj.gedcom.Indi;
import genj.gedcom.Property;
import genj.gedcom.PropertyFamilySpouse;
import genj.gedcom.PropertyHusband;
import genj.gedcom.PropertyWife;
import gj.awt.geom.Path;
import gj.layout.Layout;
import gj.layout.LayoutException;
import gj.layout.tree.NodeOptions;
import gj.layout.tree.Orientation;
import gj.layout.tree.TreeLayout;
import gj.model.Arc;
import gj.model.Graph;
import gj.model.Node;
import gj.util.ModelHelper;

/**
 * Model of our tree
 */
public class Model implements Graph, GedcomListener {
  
  /** listeners */
  private List listeners = new ArrayList(3);

  /** arcs */
  private Collection arcs = new ArrayList(100);

  /** nodes */
  private Collection nodes = new ArrayList(100);

  /** bounds */
  private Rectangle2D bounds = new Rectangle2D.Double();
  
  /** the layout we use */
  private TreeLayout layout = new TreeLayout();
    
  /** whether we're vertical or not */
  private boolean isVertical = true;
    
  /** parameters */
  private double 
    padding     = 1.0D,
    widthIndis  = 3.0D,
    widthFams   = 2.8D,
    widthMarrs  = widthIndis/16,
    heightIndis = 2.0D,
    heightFams  = 1.0D,
    heightMarrs = heightIndis/16;

  /** shape of marriage rings */
  private Shape 
    shapeMarrs = calcMarriageRings(),
    shapeIndis = new Rectangle2D.Double(-widthIndis/2,-heightIndis/2,widthIndis,heightIndis),
    shapeFams  = new Rectangle2D.Double(-widthFams /2,-heightFams /2,widthFams ,heightFams );

  /** padding of nodes */
  private double[] // n, e, s, w
    padMarrsV = new double[]{ 
      (heightIndis+padding)/2 - heightMarrs/2,
      -padding/2, 
      (heightIndis+padding)/2 - heightMarrs/2,
      -padding/2 
    },
    padMarrsH = new double[]{ 
      (widthIndis+padding)/2 - widthMarrs/2,
      -padding/2, 
      (widthIndis+padding)/2 - widthMarrs/2,
      -padding/2 
    },
    padMarrs = padMarrsV,
    padFamsD = new double[]{
      -padding*0.40,
      -padding/2*0.8,      
      padding/2,
      0      
    },
    padFamsA = new double[]{
      padding/2,
      padding*0.05,      
      -padding*0.40,
      padding*0.05      
    },
    padIndis = new double[] {
      padding/2,
      padding/2,
      padding/2,
      padding/2
    };
    
  /** gedcom we're looking at */
  private Gedcom gedcom;
  
  /** the root we've used */
  private Entity root;

  /**
   * Constructor
   */
  public Model(Gedcom ged, boolean vertical) {
    gedcom = ged;
    isVertical = vertical;
  }
  
  /**
   * Sets the root
   */
  public void setRoot(Entity entity) {
    // Indi or Fam plz
    if (!(entity instanceof Indi||entity instanceof Fam)) 
      throw new IllegalArgumentException("Indi or Fam please");
    // clear old
    arcs.clear();
    nodes.clear();
    bounds.setFrame(0,0,0,0);
    // keep as root
    root = entity;
    // prepare parsers
    Parser 
      pd = new ParserDwFS(),
      pa = new ParserAwFS();
    // prepare marr padding
    padMarrs = isVertical ? padMarrsV : padMarrsH;
    // parse its descendants
    MyNode node = pd.parse(entity, null);
    // keep bounds
    Rectangle2D r = bounds.getFrame();
    Point2D p = node.getPosition();
    // parse its ancestors while preserving position
    node = pa.parse(entity, p);    
    // update bounds
    bounds.add(r);
    // notify
    fireStructureChanged();
    // done
  }
  
  /**
   * returns the orientation   */
  public boolean isVertical() {
    return isVertical;
  }
  
  /**
   * changes the orientation   */
  public void setVertical(boolean set) {
    // nothing new?
    if (isVertical==set) return;
    // change and update
    isVertical = set;
    setRoot(root);
    // done
  }
  
  /**
   * Add listener
   */
  public void addListener(ModelListener l) {
    listeners.add(l);
    if (listeners.size()==1) gedcom.addListener(this);
  }
  
  /**
   * Remove listener
   */
  public void removeListener(ModelListener l) {
    listeners.remove(l);
    if (listeners.size()==0) gedcom.removeListener(this);
  }
  
  /**
   * @see genj.gedcom.GedcomListener#handleChange(Change)
   */
  public void handleChange(Change change) {
    // FIXME: this should be more fine-grained and make sure root's still valid
    setRoot(root);
  }
  
  /**
   * Fire even
   */
  private void fireStructureChanged() {
    for (int l=listeners.size()-1; l>=0; l--) {
      ((ModelListener)listeners.get(l)).structureChanged(this);
    }
  }
  
  /**
   * Calculates marriage rings
   */
  private Shape calcMarriageRings() {
    Ellipse2D
      a = new Ellipse2D.Double(-widthMarrs+widthMarrs/4,-heightMarrs/2,widthMarrs,heightMarrs),
      b = new Ellipse2D.Double(           -widthMarrs/4,-heightMarrs/2,widthMarrs,heightMarrs);
    GeneralPath result = new GeneralPath(a);      
    result.append(b,false);
    return result;
  }
  
  /**
   * An entity by position
   */
  public Entity getEntityAt(double x, double y) {
    // loop nodes
    Iterator it = nodes.iterator();
    while (it.hasNext()) {
      MyNode node = (MyNode)it.next();
      Point2D pos = node.getPosition();
      Shape shape = node.getShape();
      if (shape!=null&&shape.getBounds2D().contains(x-pos.getX(),y-pos.getY())&&node.entity!=null) {
        return node.entity;
      }
    }
    // nothing found
    return null;
  }
  
  /**
   * @see gj.model.Graph#getArcs()
   */
  public Collection getArcs() {
    return arcs;
  }

  /**
   * @see gj.model.Graph#getBounds()
   */
  public Rectangle2D getBounds() {
    return bounds;
  }

  /**
   * @see gj.model.Graph#getNodes()
   */
  public Collection getNodes() {
    return nodes;
  }

  /**
   * A node for an entity
   */
  private class MyNode implements Node, NodeOptions {
    
    /** the entity */
    protected Entity entity;
    
    /** arcs of this entity */
    private List arcs = new ArrayList(5);
    
    /** position of this entity */
    private Point2D pos = new Point2D.Double();
    
    /** the shape */
    private Shape shape;
    
    /** padding */
    private double[] padding;
    
    /**
     * Constructor
     */
    private MyNode(Entity enTity, Shape sHape, double[] padDing) {
      // remember
      entity = enTity;
      shape = sHape;
      padding = padDing;
      // publish
      nodes.add(this);
      // done
    }
    
    /**
     * @see gj.model.Node#getArcs()
     */
    public List getArcs() {
      return arcs;
    }

    /**
     * @see gj.model.Node#getContent()
     */
    public Object getContent() {
      return entity;
    }

    /**
     * @see gj.model.Node#getPosition()
     */
    public Point2D getPosition() {
      return pos;
    }

    /**
     * @see gj.model.Node#getShape()
     */
    public Shape getShape() {
      return shape;
    }
    
    /**
     * @see gj.layout.tree.NodeOptions#getLatitude(Node, double, double)
     */
    public double getLatitude(Node node, double min, double max, Orientation o) {
      // default is centered
      return min + (max-min) * 0.5;
    }
    /**
     * @see gj.layout.tree.NodeOptions#getLongitude(Node, double, double, double, double)
     */
    public double getLongitude(Node node, double minc, double maxc, double mint, double maxt, Orientation o) {
      // default is centered
      return minc + (maxc-minc) * 0.5;
    }
    /**
     * @see gj.layout.tree.NodeOptions#getPadding(int)
     */
    public double getPadding(Node node, int dir, Orientation o) {
      if (padding==null) return 0;
      return padding[dir];
    }
  } //MyNode

  /**
   * An arc between two individuals
   */
  private class MyArc implements Arc {
    /** start */
    private MyNode start; 
    /** end */
    private MyNode end; 
    /** path */
    private Path path;
    /**
     * Constructor
     */
    private MyArc(MyNode n1, MyNode n2, boolean p) {
      // remember
      start = n1;
      end   = n2;
      if (p) path = new Path();
      // register
      n1.arcs.add(this);
      n2.arcs.add(this);
      arcs.add(this);
      // done  
    }
    /**
     * @see gj.model.Arc#getEnd()
     */
    public Node getEnd() {
      return end;
    }
    /**
     * @see gj.model.Arc#getStart()
     */
    public Node getStart() {
      return start;
    }
    /**
     * @see gj.model.Arc#getPath()
     */
    public Path getPath() {
      return path;
    }
  } //Indi2Indi

  /**
   * Parser
   */
  private abstract class Parser {
    
    /**
     * parses a tree starting from entity     * @param entity either fam or indi     */
    public MyNode parse(Entity entity, Point2D at) {
      if (root instanceof Indi)
        return parse((Indi)root, at);
      else
        return parse((Fam )root, at);
    }
    
    /**
     * parses a tree starting from an indi     */
    public abstract MyNode parse(Indi indi, Point2D at);
    
    /**
     * parses a tree starting from a family     */
    public abstract MyNode parse(Fam fam, Point2D at);
    
    /**
     * Helper that applies the layout
     */
    private void layout(MyNode root, boolean isTopDown) {
      // layout
      try {
        layout.setTopDown(isTopDown);
        layout.setBendArcs(true);
        layout.setDebug(false);
        layout.setIgnoreUnreachables(true);
        layout.setBalanceChildren(false);
        layout.setRoot(root);
        layout.setVertical(isVertical);
        layout.applyTo(Model.this);
      } catch (LayoutException e) {
        e.printStackTrace();
      }
      // done
    }
    
  } //Parser

  /**
   * Parser - Ancestors with Families   */
  private class ParserAwFS extends Parser {
    
    private final int
      CENTER = 0,
      LEFT   = 1,
      RIGHT  = 2;
      
    /**
     * @see genj.tree.Model.Parser#parse(genj.gedcom.Fam)
     */
    public MyNode parse(Fam fam, Point2D at) {
      MyNode node = iterate(fam);
      if (at!=null) node.getPosition().setLocation(at);
      super.layout(node, false);
      return node;
    }
    
    /**
     * parse a family and its ancestors
     */
    private MyNode iterate(Fam fam) {
      // node for the fam
      MyNode node = new MyNode(fam, shapeFams, padFamsA);
      // husband & wife
      int spouses = fam.getNoOfSpouses();
      new MyArc(node, iterate(fam.getHusband(), spouses==2?LEFT:CENTER), false);
      new MyArc(node, new MyNode(fam, shapeMarrs, padMarrs), false);
      new MyArc(node, iterate(fam.getWife(), spouses==2?RIGHT:CENTER), false);
      // done
      return node;
    }
    /**
     * @see genj.tree.Model.Parser#parse(genj.gedcom.Indi)
     */
    public MyNode parse(Indi indi, Point2D at) {
      MyNode node = iterate(indi, CENTER);
      if (at!=null) node.getPosition().setLocation(at);
      super.layout(node, false);
      return node;
    }
    /**
     * parse an individual and its ancestors     */
    private MyNode iterate(Indi indi, final int alignment) {
      // node for indi      
      MyNode node;
      if (alignment==CENTER) {
        node = new MyNode(indi, shapeIndis, padIndis);
      } else {
        node = new MyNode(indi, shapeIndis, padIndis) { 
          /** patching longitude */
          public double getLongitude(Node node, double minc, double maxc, double mint, double maxt, Orientation o) {
            if (alignment==RIGHT) return mint-padding/2;
            return maxt+padding/2;
          }
        };
      }
      // do we have a family we're child in?
      if (indi!=null) {
        Fam famc = indi.getFamc();
        // grab the family
        if (famc!=null) 
          new MyArc(node, iterate(famc), true);
      } 
      // done
      return node;
    }
  } //ParserAwF

  /**
   * Parser - Descendants with Families 
   */
  private class ParserDwFS extends Parser {
    
    /** the alignment offset for an individual above its 1st fam */
    private Point2D.Double alignOffsetIndiAbove1stFam = new Point2D.Double(
        ( widthFams-padding)/2 -  widthIndis -  widthMarrs/2,
      -((heightFams-padding)/2 - heightIndis - heightMarrs/2)
    );
    
    /**
     * @see genj.tree.Model.Parser#parse(genj.gedcom.Indi)
     */
    public MyNode parse(Indi indi, Point2D at) {
      // won't support at
      if (at!=null) throw new IllegalArgumentException("at is not supported");
      // parse under pivot
      MyNode pivot = new MyNode(null, null, null);
      MyNode node = iterate(indi, pivot);
      // do the layout
      super.layout(pivot, true);
      // done
      return node;
    }
    
    /**
     * parses an indi     * @param indi the indi to parse     * @param pivot all nodes of descendant are added to pivot     * @return MyNode     */
    private MyNode iterate(Indi indi, MyNode pivot) {
      // create node for indi
      MyNode node = new MyNode(indi, shapeIndis, padIndis) { 
        /** patch latitude */
        public double getLongitude(Node node, double minc, double maxc, double mint, double maxt, Orientation o) {
          return minc + o.getLongitude( alignOffsetIndiAbove1stFam );
        }
      };
      new MyArc(pivot, node, false);
      // loop through our fams
      Fam[] fams = indi.getFamilies();
      MyNode fam1 = null;
      for (int f=0; f<fams.length; f++) {
        // add arc : node-fam
        MyNode fami = iterate(fams[f], fam1);
        if (fam1==null) fam1 = fami;
        new MyArc(node, fami, false);
        // add arcs " pivot-marr, pivot-spouse
        new MyArc(pivot, new MyNode(fams[f], shapeMarrs, padMarrs), false);
        new MyArc(pivot, new MyNode(fams[f].getOtherSpouse(indi), shapeIndis, padIndis), false);
        // next family
      }
      // done
      return node;
    }

    /**
     * @see genj.tree.Model.Parser#parse(genj.gedcom.Fam)
     */
    public MyNode parse(Fam fam, Point2D at) {
      MyNode node = iterate(fam, null);
      if (at!=null) node.getPosition().setLocation(at);
      super.layout(node, true);
      return node;
    }
    
    /**
     * parses a fam and its descendants
     * @parm pivot all nodes of descendant are added to pivot     */
    private MyNode iterate(Fam fam, MyNode pivot) {
      // node for fam
      MyNode node = new MyNode(fam, shapeFams, padFamsD);
      // pivot is me if unset
      if (pivot==null) pivot = node;
      // grab the children
      Indi[] children = fam.getChildren();
      for (int c=0; c<children.length; c++) {
        // create an arc from node to node for indi
        Indi child = children[c];
        new MyArc(pivot, iterate(child, pivot), true);       
        // next child
      }
      // done
      return node;
    }
    
  } //ParserDwF
  
} //Model
