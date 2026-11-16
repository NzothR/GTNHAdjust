package com.gtnhadjust.mixins.terminal;

import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import codechicken.nei.bookmark.BookmarkGrid;
import codechicken.nei.bookmark.BookmarkGroup;
import codechicken.nei.bookmark.BookmarkItem;

@Mixin(value = BookmarkGrid.class, remap = false)
public interface BookmarkGridAccessor {

    @Accessor("bookmarkItems")
    List<BookmarkItem> gtnhadjust$items();

    @Accessor("groups")
    Map<Integer, BookmarkGroup> gtnhadjust$groups();
}
