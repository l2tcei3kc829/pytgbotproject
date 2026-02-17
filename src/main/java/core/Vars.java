package core;

import arc.struct.Seq;
import arc.struct.StringMap;
import core.bot.markups.MarkupManager;
import core.config.Config;
import core.info.DictPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;


public class Vars {
    public static Logger log = LoggerFactory.getLogger("MAIN");
    public static TelegramLongPollingBot bot;
    public static MarkupManager manager;
    public static Seq<DictPart> dicts = new Seq<>();
    public static StringMap id2desc = new StringMap();
    public static Config config;
}
