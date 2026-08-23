package com.lelqwq.gtmpi;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

/** gtmpi.cfg —— MTE ID 偏移。与其他附加模组 ID 冲突时改配置即可，无需改代码。 */
public class Config {

    public static int metaIdOffset = 0;

    public static void load(File file) {
        Configuration config = new Configuration(file);
        config.load();
        metaIdOffset = config.getInt(
            "metaIdOffset",
            Configuration.CATEGORY_GENERAL,
            0,
            0,
            1000,
            "MTE ID 偏移（最终 ID = 18000 + 偏移 + 相对 ID）");
        if (config.hasChanged()) {
            config.save();
        }
    }
}
