/**
 * JUNIT TESTCASE - DONT PACKAGE FOR DISTRIBUTION
 */
package genj.gedcom;

import junit.framework.TestCase;

/**
 * Test gedcom TagPaths
 */
public class TagPathTest extends TestCase {

  /**
   * Test path matches that include selectors
   */
  public void testPathMatches() {
    
    // Match a simple zero-index element of a path with a tag
    testPathMatch("FOO"  , "FOO"  ,true);
    testPathMatch("FOO#0", "FOO"  ,true);
    testPathMatch("FOO#1", "FOO"  ,true);
    testPathMatch("FOO"  , "FOOO" ,false);
    testPathMatch("FOO#0", "FO"   ,false);
    testPathMatch("FOO#1", "FOOOO",false);

    // Match a simple zero-index element of a path with a tag and selector
    // In a path with element "FOO", this corresponds to "FOO#0"
    // 
    testPathMatch("FOO"  , "FOO", 0, TagPath.MATCH_ALL);
    testPathMatch("FOO#0", "FOO", 0, TagPath.MATCH_ALL);
    testPathMatch("FOO#0", "FOO", 1, TagPath.MATCH_TAG);
    testPathMatch("FOO#1", "FOO", 0, TagPath.MATCH_TAG);
    testPathMatch("FOO#1", "FOO", 1, TagPath.MATCH_ALL);
    
    testPathMatch("FOO"  , "BAR", 0, TagPath.MATCH_NONE);
    testPathMatch("FOO"  , "FOOO",0, TagPath.MATCH_NONE);
    testPathMatch("FOO#0", "FO" , 0, TagPath.MATCH_NONE);
    testPathMatch("FOO#1", "FOOO",1, TagPath.MATCH_NONE);
  }

  /**
   * test helper
   */
  private void testPathMatch(String path, String tag, boolean result) {

    assertEquals(path+".match(0,"+tag+")", new TagPath(path).match(0, tag), result);

  }
  
  /**
   * test helper
   */
  private void testPathMatch(String path, String tag, int selector, int result) {
  
    assertEquals(path+".match(0,"+tag+","+selector+")", new TagPath(path).match(0, tag, selector), result);
    
  }


} //AnselCharsetTest
