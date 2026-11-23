package com.lelqwq.gtmpi.common.api.enums;

import com.lelqwq.gtmpi.Config;

/**
 * 本 mod 的 MTE ID 分配。
 *
 * <p>
 * 最终 ID = BASE (18000) + Config.metaIdOffset（配置偏移量）+ 相对 ID。
 * BASE = 18000：已对照 GT5U 5.09.54.275 全仓库唯一的 MTE ID 枚举
 * （gregtech.api.enums.MetaTileEntityIDs）验证，17000–29989 区段完全空白。
 */
public enum MetaTileEntityID {

    /** 巨型样板输入总线（物品，Tier 6） */
    MEGA_CRAFTING_INPUT_BUS(0),
    /** 巨型样板输入总成（防水、支持流体样板，Tier 10） */
    MEGA_CRAFTING_INPUT_BUFFER(1),
    /** 巨型样板输入镜像（连接总成，Tier 11） */
    MEGA_CRAFTING_INPUT_PROXY(2);

    /** 最终计算出的全局唯一 ID */
    public final int ID;

    /** ID 基准值 */
    private static final int BASE = 18000;

    MetaTileEntityID(int relativeId) {
        this.ID = BASE + Config.metaIdOffset + relativeId;
    }
}
