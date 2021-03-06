/**
 * JUNIT TESTCASE - DONT PACKAGE FOR DISTRIBUTION
 */
package genj.io;

import genj.gedcom.Gedcom;
import genj.gedcom.GedcomException;
import genj.gedcom.Options;
import genj.util.Origin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import junit.framework.TestCase;

/**
 * Testing Gedcom read/write diff
 */
public class GedcomReadWriteTest extends TestCase {

  final static Logger LOG = Logger.getLogger("test.gedcom.readwrite");
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    Options.getInstance().setValueLineBreak(255);
    
  }
  
  /**
   * Read a stress file
   */
  public void testStressFile() throws IOException, GedcomException {
    
    // try to read file
    Gedcom ged = GedcomReaderFactory.createReader(getClass().getResourceAsStream("stress.ged"), null).read();
    
    // write it again
    File temp = File.createTempFile("test", ".ged");
    OutputStream out = new FileOutputStream(temp);
    new GedcomWriter(ged, out).write();
    out.close();
    
    // compare line by line
    assertEquals(Collections.singletonList("2 _TAG<>"), diff(temp, getClass().getResourceAsStream("stress.ged")) );
  }
  
  /**
   * Read a file / write it / compare
   */
  @SuppressWarnings("deprecation")
  public void testReadWrite() throws IOException, GedcomException {
    
    // read/write file
    File original = new File("./gedcom/royal92.ged");
    File temp = File.createTempFile("test", ".ged");
    LOG.info("Temp file generated :"+temp);
    
    
    // read it
    Gedcom ged = GedcomReaderFactory.createReader(Origin.create(original.toURL()), null).read();
    
    // write it
    FileOutputStream out = new FileOutputStream(temp);
    new GedcomWriter(ged, out).write();
    out.close();
    
    // diff files and there should be one difference
    assertEquals(original + " <> " + temp, Collections.EMPTY_LIST, diff(original, temp));
    
  }
  
  private List<String> diff(File file1, File file2) throws IOException {
    return diff(file1, new FileInputStream(file2));
  }
    
  private List<String> diff(File file1, InputStream file2) throws IOException {
    
    List<String> result = new ArrayList<String>();
    
    BufferedReader left = new BufferedReader(new InputStreamReader(new FileInputStream(file1)));
    BufferedReader right = new BufferedReader(new InputStreamReader(file2));
    
    // read past the header
    String lineLeft = left.readLine();
    while (true) {
      left.mark(256);
      lineLeft = left.readLine();
      if (lineLeft==null)
        throw new Error();
      if (lineLeft.startsWith("0")) break;
    }
    left.reset();
    String lineRight = right.readLine();
    while (true) {
      right.mark(256);
      lineRight = right.readLine();
      if (lineRight==null)
        throw new Error();
      if (lineRight.startsWith("0")) break;
    }
    right.reset();
    
    // compare records
    while (true) {
      
      left.mark(256); right.mark(256);
      lineLeft = left.readLine();
      lineRight = right.readLine();

      // done?
      if (lineLeft==null&&lineRight==null)
        break;
      
      // assume "," equals ", "
      if (lineLeft==null||lineRight==null) {
        result.add(lineLeft+"<>"+lineRight);
        break;
      }
        
      // assert equal
      if (!matches(lineLeft, lineRight)) {
        
        LOG.warning(lineLeft + "<>" + lineRight);
        
        // maybe next line matches again?
        left.mark(256);
        if (matches(left.readLine(), lineRight))
          result.add(lineLeft+"<>");
        else
          result.add(lineLeft+"<>"+lineRight);
        left.reset();
        right.reset();
      }
    }
    
    left.close();
    right.close();
    
    // done
    return result;
  }
  
  private static Pattern COMMASPACE = Pattern.compile(", ");
  private static String COMMA = ",";

  private boolean matches(String left, String right) {
    if (left==null||right==null)
      return false;
    return COMMASPACE.matcher(left).replaceAll(COMMA).equals(COMMASPACE.matcher(right).replaceAll(COMMA));
  }
  
} //GedcomIDTest
