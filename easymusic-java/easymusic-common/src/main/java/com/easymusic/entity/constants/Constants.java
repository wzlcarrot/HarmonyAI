package com.easymusic.entity.constants;

public class Constants {
    public static final String ZERO_STR = "0";

    public static final Integer ZERO = 0;

    public static final Integer ONE = 1;

    public static final Integer LENGTH_5 = 5;
    public static final Integer LENGTH_8 = 8;
    public static final Integer LENGTH_14 = 14;
    public static final Integer LENGTH_15 = 15;
    public static final Integer LENGTH_12 = 12;
    public static final Integer LENGTH_20 = 20;

    public static final Integer LENGTH_30 = 30;

    public static final String[] IMAGES_SUFFIX = {".jpeg", ".jpg", ".png", ".gif", ".bmp", ".webp"};

    public static final String FILE_FOLDER_FILE = "file/";

    public static final String FILE_FOLDER_TEMP = "/temp/";

    public static final String FILE_FOLDER_AVATAR_NAME = "avatar/";

    public static final String IMAGE_SUFFIX = ".jpg";

    public static final String AUDIO_SUFFIX = ".mp3";

    public static final String DEFAULT_AVATAR_PATH = "/avatar/avatars.jpg";

    public static final String AVATAR_SUFIX = ".png";

    public static final Integer ORDER_TIMEOUT_MIN = 10;

    public static final String FILE_FOLDER = "file123/";

    /**
     * redis key 相关
     */

    /**
     * 过期时间 1分钟
     */
    public static final Integer REDIS_KEY_EXPIRES_ONE_MIN = 60;

    /**
     * 过期时间 1天
     */
    public static final Integer REDIS_KEY_EXPIRES_DAY = REDIS_KEY_EXPIRES_ONE_MIN * 60 * 24;

    private static final String REDIS_KEY_PREFIX = "easymusic:";

    public static final String REDIS_KEY_CHECK_CODE = REDIS_KEY_PREFIX + "checkcode:";

    public static final String REDIS_KEY_TOKEN_WEB_USER = REDIS_KEY_PREFIX + "token:";

    public static final String REDIS_KEY_TOKEN_ADMIN_USER = REDIS_KEY_PREFIX + "token:admin:";

    public static final String REDIS_KEY_MUSIC_CREATE_QUEUE = REDIS_KEY_PREFIX + "create:queue:";

    public static final String REDIS_KEY_SYS_DICT = REDIS_KEY_PREFIX + "sysdict:";

    public static final String REDIS_KEY_ORDER_DELAY_QUEUE = REDIS_KEY_PREFIX + "order:delay:queue:";

    public static final String REDIS_KEY_ORDER_HAVE_PAY = REDIS_KEY_PREFIX + "order:havepay:";

    public static final String REDIS_KEY_QR_CODE = REDIS_KEY_PREFIX + "qr:code:";

    /**
     * 首页音乐列表缓存相关
     */
    public static final String REDIS_KEY_MUSIC_COMMEND_LIST = REDIS_KEY_PREFIX + "music:commend:list:";
    public static final String REDIS_KEY_MUSIC_LATEST_PAGE = REDIS_KEY_PREFIX + "music:latest:page:";

    public static final String REDIS_KEY_ORDER_EXPIRE_TIME = REDIS_KEY_PREFIX + "order:expire:";
    public static final String REDIS_KEY_MUSIC_CREATE_SEMAPHORE = REDIS_KEY_PREFIX + "create:semaphore:";

    /**
     * 缓存相关key（用于布隆过滤器配合缓存使用）
     */
    public static final String REDIS_KEY_MUSIC_INFO = REDIS_KEY_PREFIX + "music:info:";
    public static final String REDIS_KEY_USER_INFO = REDIS_KEY_PREFIX + "user:info:";
    public static final String REDIS_KEY_ORDER_INFO = REDIS_KEY_PREFIX + "order:info:";

    public static final String TOKEN_WEB = "easymusic_token";    //cookie中的session_token键;

    /**
     * 音乐生成并发控制
     */
    public static final int MUSIC_CREATE_MAX_PERMITS = 10;
    public static final long MUSIC_CREATE_SEMAPHORE_WAIT_MS = 200;
    public static final long MUSIC_CREATE_SEMAPHORE_EXPIRE_SECONDS = 30;

    /**
     * RabbitMQ 相关常量
     */
    // 主队列相关
    public static final String RABBITMQ_EXCHANGE_MUSIC_CREATE = "music.create.exchange";
    public static final String RABBITMQ_QUEUE_MUSIC_CREATE = "music.create.queue";
    public static final String RABBITMQ_ROUTING_KEY_MUSIC_CREATE = "music.create.routing";
    
    // 死信队列相关
    public static final String RABBITMQ_EXCHANGE_MUSIC_CREATE_DLX = "music.create.exchange.dlx";
    public static final String RABBITMQ_QUEUE_MUSIC_CREATE_DLX = "music.create.queue.dlx";
    public static final String RABBITMQ_ROUTING_KEY_MUSIC_CREATE_DLX = "music.create.routing.dlx";

}
