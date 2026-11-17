package com.gtnhadjust.machine.proxy;

import java.util.List;
import java.util.Locale;

import com.asdflj.nech.API;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Search is purely client-side and follows the installed NECH spelling configuration. */
@SideOnly(Side.CLIENT)
public final class ProxyLocatorSearch {

    private static final boolean NECH = Loader.isModLoaded("nech");

    private ProxyLocatorSearch() {}

    public static boolean matches(List<String> names, String search) {
        String query = normalize(search).trim();
        for (String name : names) {
            String text = normalize(name);
            if (text.contains(query) || (NECH && API.INSTANCE.contains(text, query))) return true;
        }
        return false;
    }

    private static String normalize(String text) {
        return text.replaceAll("(?i)§[0-9a-fk-or]", "")
            .toLowerCase(Locale.ROOT);
    }
}
