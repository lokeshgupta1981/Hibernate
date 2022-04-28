package com.howtodoinjava.demo.mappings;

import com.howtodoinjava.demo.mappings.manytomany.TestManyToManyJoinTableAssociation;
import com.howtodoinjava.demo.mappings.onetomany.TestOneToManyForeignKeyAssociation;
import com.howtodoinjava.demo.mappings.onetomany.TestOneToManyJoinTableAssociation;
import com.howtodoinjava.demo.mappings.onetotone.TestForeignKeyAssociation;
import com.howtodoinjava.demo.mappings.onetotone.TestJoinTableAssociation;
import com.howtodoinjava.demo.mappings.onetotone.TestMapsByIdAssociation;
import com.howtodoinjava.demo.mappings.onetotone.TestSharedPrimaryKeyAssociation;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({TestMapsByIdAssociation.class,
    TestForeignKeyAssociation.class,
    TestJoinTableAssociation.class,
    TestSharedPrimaryKeyAssociation.class,
    TestOneToManyForeignKeyAssociation.class,
    TestOneToManyJoinTableAssociation.class,
    TestManyToManyJoinTableAssociation.class})
public class AppTestSuite {
}
