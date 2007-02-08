/**
 * Reports are Freeware Code Snippets
 *
 * This report is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * This is pretty much a brute force solution to this problem.
 * There is a 6 x 8 grid which spreads out various ancestors.
 * In hopes of devising a clever way to do this, I created a utility
 * ask for ancesters as if I was using the Swedish denotation:
 * moormoor is mother's mother. So, MMMM is the maternal great 
 * great grandmother. The code as it stands is kind of sexist.
 * Only fathers are in the corners.
 *
 *   FFFF  FFF   FFFM      MFFM  MFF   MFFF
 *
 *   FFMF                              MFMF
 *   FFM   FF                    MF    MFM
 *   FFMM                              MFMM
 *         F                     M
 *   FMFM                              MMFM
 *   FMF   FM                    MM    MMF
 *   FMFF                              MMFF
 *
 *   FMMF  FMM   FMMM      MMMM  MMM   MMMF  
 *
 * TODO: get links out of the GEDCOM and put them in.
 *
 * __b: xx-mm-yyyy d: dd-mm-yyyy__
 * 1234567890123456789012345678901
 */
import genj.gedcom.Fam;
import genj.gedcom.Gedcom;
import genj.gedcom.Indi;
import genj.gedcom.Property;
import genj.gedcom.TagPath;
import genj.report.Report;
import genj.report.ReportBridge;

/**
 * GenJ -  ReportAncestors
 * @version 0.1
 */
public class Report4Generations implements Report {

  /** this report's version */
  public static final String VERSION = "0.1";
    public static final int BLOCK_WIDTH = 34;
  /**
   * Returns the version of this script
   */
  public String getVersion() {
    return VERSION;
  }
  
  /**
   * Returns the name of this report - should be localized.
   */
  public String getName() {
    return "Four Generations Chart";
  }  

  /**
   * Some information about this report
   * @return Information as String
   */
  public String getInfo() {
    return "Make a chart of four generations of ancestors of an individual";
  }

  /**
   * Indication of how this reports shows information
   * to the user. Standard Out here only.
   */
  public boolean usesStandardOut() {
    return true;
  }

  /**
   * Author
   */
  public String getAuthor() {
    return "YON - Jan C. Hardenbergh";
  }

  /**
   * Tells whether this report doesn't change information in the Gedcom-file
   */
  public boolean isReadOnly() {
    return true;
  }

  /**
   * This method actually starts this report
   */
  public boolean start(ReportBridge bridge, Gedcom gedcom) {

    // Show the users in a combo to the user
    Indi indi = (Indi)bridge.getValueFromUser(
      "Please select an individual",
      gedcom.getEntities(Gedcom.INDIVIDUALS).toArray(),
      null
    );
    
    if (indi==null) {
      return false;
    }
    
    Indi indiffff = ancestor(bridge, indi, "FFFF");
    Indi indifff  = ancestor(bridge, indi, "FFF");
    Indi indifffm = ancestor(bridge, indi, "FFFM");
    Indi indimffm = ancestor(bridge, indi, "MFFM");
    Indi indimff  = ancestor(bridge, indi, "MFF");
    Indi indimfff = ancestor(bridge, indi, "MFFF");

    bridge.println(lineone(indiffff)+lineone(indifff)+lineone(indifffm)+lineone(indimffm)+lineone(indimff)+lineone(indimfff));
    bridge.println(linetwo(indiffff,false,(indiffff!=null))
		   +linetwo(indifff,(indiffff!=null),(indifffm!=null),(indifff!=null))
		   +linetwo(indifffm,(indifffm!=null),false)
		   +linetwo(indimffm,false,(indimffm!=null))
		   +linetwo(indimff,(indimffm!=null),(indimfff!=null),(indimff!=null))
		   +linetwo(indimfff,(indimfff!=null),false));

    String spacer1 = pad(BLOCK_WIDTH)+connect(indifff!=null)+pad(BLOCK_WIDTH)+pad(BLOCK_WIDTH)+connect(indimff!=null);
    bridge.println(spacer1); bridge.println(spacer1); bridge.println(spacer1);

    Indi indiffmf = ancestor(bridge, indi, "FFMF");
    Indi indimfmf = ancestor(bridge, indi, "MFMF");

    bridge.println(lineone(indiffmf)+connect(indifff!=null)+pad(BLOCK_WIDTH)+pad(BLOCK_WIDTH)+connect(indimff!=null)+lineone(indimfmf));
    bridge.println(linetwo(indiffmf,false,false,(indiffmf!=null))+connect(indifff!=null)+pad(BLOCK_WIDTH)+pad(BLOCK_WIDTH)+connect(indimff!=null)+linetwo(indimfmf,false,false,(indimfmf!=null)));

    String spacer2 = connect(indiffmf!=null)+connect(indifff!=null)+pad(BLOCK_WIDTH)+pad(BLOCK_WIDTH)+connect(indimff!=null)+connect(indimfmf!=null);
    bridge.println(spacer2); bridge.println(spacer2);


    // *   MBNH    Collis               Arthur  Lily
    
    Indi indiffm = ancestor(bridge, indi, "FFM");
    Indi indiff  = ancestor(bridge, indi, "FF");
    Indi indimf  = ancestor(bridge, indi, "MF");
    Indi indimfm = ancestor(bridge, indi, "MFM");

    bridge.println(lineone(indiffm)+lineone(indiff)+pad(BLOCK_WIDTH)+pad(BLOCK_WIDTH)+lineone(indimf)+lineone(indimfm));
    bridge.println(linetwo(indiffm,false,true,(indiffm!=null))+linetwo(indiff,(indiffm!=null),false,(indiff!=null))+pad(BLOCK_WIDTH)+pad(BLOCK_WIDTH)+linetwo(indimf,false,(indimfm!=null),(indimf!=null))+linetwo(indimfm,(indimfm!=null),false));


    // *   Alice                                MFMM
    Indi indiffmm  = ancestor(bridge, indi, "FFMM");
    Indi indimfmm  = ancestor(bridge, indi, "MFMM");

    String spacer3 = connect(indiffmm!=null)+connect(indiff!=null)+pad(BLOCK_WIDTH)+pad(BLOCK_WIDTH)+connect(indimf!=null)+connect(indimfmm!=null);
    bridge.println(spacer3); bridge.println(spacer3);

    // FFMM
    bridge.println(lineone(indiffmm)+connect()+pad(BLOCK_WIDTH)+pad(BLOCK_WIDTH)+connect()+lineone(indimfmm));
    bridge.println(linetwo(indiffmm,false,false)+connect(indiff!=null)+pad(BLOCK_WIDTH)+pad(BLOCK_WIDTH)+connect(indimf!=null)+linetwo(indimfmm,false,false));
    String spacer4 = pad(BLOCK_WIDTH)+connect(indiff!=null)+pad(BLOCK_WIDTH)+pad(BLOCK_WIDTH)+connect(indimf!=null);
    bridge.println(spacer4); bridge.println(spacer4);


    // *            Jan                  Gail
    Indi indif  = ancestor(bridge, indi, "F");
    Indi indim  = ancestor(bridge, indi, "M");

    bridge.println(pad(BLOCK_WIDTH)+lineone(indif)+pad(BLOCK_WIDTH/2)+lineone(indi)+pad(BLOCK_WIDTH/2)+lineone(indim)+pad(BLOCK_WIDTH));
    bridge.println(pad(BLOCK_WIDTH)+linetwo(indif,false,true)+pad(BLOCK_WIDTH/2,true)+linetwo(indi,true,true)+pad(BLOCK_WIDTH/2,true)+linetwo(indim,true,false)+pad(BLOCK_WIDTH));

    // needed now for spacers
    Indi indifm  = ancestor(bridge, indi, "FM");
    Indi indimm  = ancestor(bridge, indi, "MM");

    // spacers now above
    String spacer5 = pad(BLOCK_WIDTH)+connect(indifm!=null)+pad(BLOCK_WIDTH)+pad(BLOCK_WIDTH)+connect(indimm!=null);
    bridge.println(spacer5); bridge.println(spacer5);

    // *   Ida      c                       c      MMFM
    Indi indifmfm  = ancestor(bridge, indi, "FMFM");
    Indi indimmfm  = ancestor(bridge, indi, "MMFM");

    bridge.println(lineone(indifmfm)+connect(indifm!=null)+pad(BLOCK_WIDTH)+pad(BLOCK_WIDTH)+connect(indimm!=null)+lineone(indimmfm));
    bridge.println(linetwo(indifmfm,false,false,(indifmfm!=null))+connect(indifm!=null)+pad(BLOCK_WIDTH)+pad(BLOCK_WIDTH)+connect(indimm!=null)+linetwo(indimmfm,false,false,(indimmfm!=null)));

    //      c       c                       c       c 
    // *   Alan c   Nancy                Mary Jane c John
    String spacer6 = connect(indifmfm!=null)+connect(indifm!=null)+pad(BLOCK_WIDTH)+pad(BLOCK_WIDTH)+connect(indifm!=null)+connect(indimmfm!=null);
    bridge.println(spacer6); bridge.println(spacer6);

    Indi indifmf = ancestor(bridge, indi, "FMF");
    Indi indimmf = ancestor(bridge, indi, "MMF");

    bridge.println(lineone(indifmf)+lineone(indifm)+pad(BLOCK_WIDTH)+pad(BLOCK_WIDTH)+lineone(indimm)+lineone(indimmf));
    bridge.println(linetwo(indifmf,false,(indifmf!=null))+linetwo(indifm,(indifmf!=null),false)+pad(BLOCK_WIDTH)+pad(BLOCK_WIDTH)+linetwo(indimm,false,(indimmf!=null))+linetwo(indimmf,(indimmf!=null),false));

    // need these to decide about connectors
    Indi indifmm  = ancestor(bridge, indi, "FMM");
    Indi indimmm  = ancestor(bridge, indi, "MMM");
    //      c       c                       c       c 
    // *   James    c                       c      MMFF
    String spacer7 = connect(indifmf!=null)+connect(indifmm!=null)+pad(BLOCK_WIDTH)+pad(BLOCK_WIDTH)+connect(indimmm!=null)+connect(indimmf!=null);
    bridge.println(spacer7); bridge.println(spacer7);



    Indi indifmff = ancestor(bridge, indi, "FMFF");
    Indi indimmff = ancestor(bridge, indi, "MMFF");
    bridge.println(lineone(indifmff)+connect(indifmm!=null)+pad(BLOCK_WIDTH)+pad(BLOCK_WIDTH)+connect(indimmm!=null)+lineone(indimmff));
    bridge.println(linetwo(indifmff,false,false)+connect(indifmm!=null)+pad(BLOCK_WIDTH)+pad(BLOCK_WIDTH)+connect(indimmm!=null)+linetwo(indimmff,false,false));

    //              c                       c   
    // *   Henry   Ruth  Bessie      MMMM Marie     MMMF
    
    Indi indifmmf = ancestor(bridge, indi, "FMMF");
    Indi indifmmm = ancestor(bridge, indi, "FMMM");
    Indi indimmmm = ancestor(bridge, indi, "MMMM");
    Indi indimmmf = ancestor(bridge, indi, "MMMF");

    String spacer8 = pad(BLOCK_WIDTH)+connect(indifmm!=null)+pad(BLOCK_WIDTH)+pad(BLOCK_WIDTH)+connect(indimmm!=null);
    bridge.println(spacer8); bridge.println(spacer8); bridge.println(spacer8);

    bridge.println(lineone(indifmmf)+lineone(indifmm)+lineone(indifmmm)
		   +lineone(indimmmm)+lineone(indimmm)+lineone(indimmmf));
    bridge.println(linetwo(indifmmf,false,(indifmmf!=null))
		   +linetwo(indifmm,(indifmmf!=null),(indifmmm!=null))
		   +linetwo(indifmmm,(indifmmm!=null),false)
		   +linetwo(indimmmm,false,(indimmmm!=null))
		   +linetwo(indimmm,(indimmmm!=null),(indimmmf!=null))
		   +linetwo(indimmmf,(indimmmf!=null),false));


    // Done
    return true;
  }
  
  /**
   * parent - prints information about one parent and then recurses
   */
    private Indi ancestor(ReportBridge bridge, Indi indi, String path) {

	//	bridge.println("debug: "+path+" "); 

	if (path == null || indi == null)
	    return null;
    
	Fam famc = indi.getFamc();

	if (famc==null) {
	    //  bridge.println("no Famc "+ format(indi));
	    return null;
	}

	if (path.charAt(0) == 'F' && famc.getHusband()!=null) {
	    if (path.length() == 1) {
		//bridge.println(format(indi));
		return famc.getHusband();
	    } else
		return ancestor(bridge, famc.getHusband(), path.substring(1));
	} else if (path.charAt(0) == 'M' && famc.getWife()!=null) {
	    if (path.length() == 1) {
		//bridge.println(format(indi));
		return famc.getWife();
	    } else
		return ancestor(bridge, famc.getWife(), path.substring(1));
	}
	return null;
    }
  
  /**
   * resolves the information of one Indi
   */
  private String format(Indi indi) {
    
    // Might be null
    if (indi==null) {
      return "?";
    }
    
    // name
    String n = indi.getName();
    
    // birth?
    String b = " b: " + indi.getBirthAsString();
    
    // death?
    String d = " d: " + indi.getDeathAsString();
    
    Property place = indi.getProperty(new TagPath("INDI:BIRT:PLAC"),true);

    // 
    Property propTitle = indi.getProperty(new TagPath("INDI:TITL"),true);

    // String t = indi.getProperty(new TagPath("INDI:TITL"),true).toString();
    String title = (propTitle == null)?"":propTitle.toString();
    if (title.length() > 0)
	title = " TITL: "+title;
    return n + b + d + " PLAC: "+place+title;
  }


  private String lineone(Indi indi) {
    
    // Might be null
    if (indi==null) {
      return pad(BLOCK_WIDTH);
    }
    
    // name
    String n = indi.getName();
    // name
    String l = indi.getLastName();
    String name;

    int offset = l.length() + 2;
    if (offset >= n.length())
	name = n;
    else
	name = n.substring(l.length()+2) + " "+ l;

    Property propTitle = indi.getProperty(new TagPath("INDI:TITL"),true);
    
    // String t = indi.getProperty(new TagPath("INDI:TITL"),true).toString();
    if (propTitle != null)
	name = propTitle.toString()+" "+name;

    int padding = BLOCK_WIDTH - name.length();
    String padded = pad(padding/2)+name+pad(padding-padding/2);
    // here's the result 
    return padded;
    
    // Could be a hyperlink, too
    //return "<a href=\"\">" + indi.getName() + "</a>" + b + d;
  }

  private String linetwo(Indi indi, boolean left, boolean right) {
      return linetwo(indi, left, right, false);
  }

  private String linetwo(Indi indi, boolean left, boolean right, boolean lower) {
    if (indi==null) {
      return pad(BLOCK_WIDTH);
    }
    // birth?
    String dates = "";

    if (indi.getBirthAsString().length() > 0)
	dates = " b: " + indi.getBirthAsString();
    if (indi.getDeathAsString().length() > 0)
	dates += " d: " + indi.getDeathAsString();

    if (dates.length() <= 0) {
	if (left)
	    dates = "-+";
	else if (right)
	    dates = " +";
	else if (lower)
	    dates = " |";
	else
	    dates = "  ";
    } else
	dates += " ";
    
    int padding = BLOCK_WIDTH - dates.length();
    String padded = pad(padding/2,left)+dates+pad(padding-padding/2,right);
    // here's the result 
    return padded;
  }

    private String pad(int count) {return pad(count,false);}
  /**
   * padding 
   */
  private String pad(int count, boolean dash) {
      String spaces = "                                              "; 
      String dashes = "----------------------------------------------"; 
      int max = spaces.length();
      if (count < 1)
	  count = 1;
      if (count > max)
	  count = max;
      return (dash)?dashes.substring(max-count):spaces.substring(max-count);
  }
  
    private String connect() {
	return connect(true);
    }
    private String connect(boolean line) {
	if (line)
	    return pad(BLOCK_WIDTH/2)+"|"+pad(BLOCK_WIDTH/2-1);
	else
	    return pad(BLOCK_WIDTH);
    }
}

