package com.clevercloud.biscuit.datalog;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class WorldTest extends TestCase {
   public WorldTest(String testName) {
      super(testName);
   }

   public static Test suite() {
      return new TestSuite(WorldTest.class);
   }

   public void testWorld() {
      final World world = new World();
   }
}
