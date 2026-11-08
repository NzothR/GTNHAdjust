package com.gtnhadjust.machine.proxy;

final class ProxyRecipeDuration {

    private ProxyRecipeDuration() {}

    static double factor(String recipeMap, int glassTier) {
        if (!"otht.recipe.TSSF".equals(recipeMap)) return 1.0;
        double tier = Math.max(1, glassTier);
        return 1.0 / (tier * tier);
    }
}
