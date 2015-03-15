package com.xiantrimble.graphene.junit;

import java.util.function.Supplier;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.xiantrimble.graphene.Graphene;

public interface GrapheneRule extends TestRule, Supplier<Graphene> {

}
