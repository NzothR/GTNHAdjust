package com.futureresourceprotocol.recipe;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class RecipeListSnapshotTest {

    @Test
    public void removesMatchingRecipesAndRestoresOriginalOrder() {
        List<String> recipes = new ArrayList<>(Arrays.asList("other-a", "uhv", "other-b", "umv", "other-c"));
        RecipeListSnapshot<String> snapshot = new RecipeListSnapshot<>();
        assertEquals(2, snapshot.remove(recipes, s -> s.equals("uhv") || s.equals("umv")));
        assertEquals(Arrays.asList("other-a", "other-b", "other-c"), recipes);
        snapshot.restore(recipes);
        assertEquals(Arrays.asList("other-a", "uhv", "other-b", "umv", "other-c"), recipes);
        snapshot.restore(recipes);
        assertEquals(5, recipes.size());
    }
}
