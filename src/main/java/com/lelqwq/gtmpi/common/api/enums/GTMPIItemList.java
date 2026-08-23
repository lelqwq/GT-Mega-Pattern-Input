package com.lelqwq.gtmpi.common.api.enums;

import net.minecraft.item.ItemStack;

import gregtech.api.util.GTUtility;

/**
 * 模组物品统一索引枚举：机器物品在 {@code sAfterGTPreload} 注册阶段绑定，
 * 之后可安全地通过 {@link #get(int)} 获取物品堆栈。
 */
public enum GTMPIItemList {

    /** 巨型样板输入总线（物品） */
    Mega_Crafting_Input_Bus,
    /** 巨型样板输入总成（防水、流体样板） */
    Mega_Crafting_Input_Buffer,
    /** 巨型样板输入镜像 */
    Mega_Crafting_Input_Proxy;

    private ItemStack mStack;

    /** 绑定物品堆栈（保留 1 个副本）。 */
    public GTMPIItemList set(ItemStack aStack) {
        mStack = aStack == null ? null : GTUtility.copyAmount(1, aStack);
        return this;
    }

    /** 获取指定数量的物品堆栈；未绑定或数量为 0 时返回 null。 */
    public ItemStack get(int aAmount) {
        if (mStack == null || aAmount <= 0) return null;
        return GTUtility.copyAmount(aAmount, mStack);
    }
}
