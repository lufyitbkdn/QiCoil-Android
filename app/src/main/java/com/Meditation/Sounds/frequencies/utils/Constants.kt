package com.Meditation.Sounds.frequencies.utils

import com.Meditation.Sounds.frequencies.lemeor.data.model.Track

/**
 * Created by Admin on 3/9/2018.
 */

class Constants {



    companion object {
        @JvmStatic
        val CHARSET = "UTF-8"
        @JvmField
        var PREF_SESSION_ID = "PREF_SESSION_ID"
        @JvmStatic
        var PREF_USER_ID = "PREF_USER_ID"
        @JvmStatic
        var FAILURE_SESSION_EXPIRED = "Unauthorized"
        @JvmStatic
        var BROADCAST_ADD_SONG = "BROADCAST_ADD_SONG"
        @JvmStatic
        var BROADCAST_ADD_SONG_TO_ALBUM = "BROADCAST_ADD_SONG_TO_ALBUM"
        @JvmStatic
        var BROADCAST_BACK_ALBUM_FROM_PHONE = "BROADCAST_BACK_ALBUM_FROM_PHONE"
        @JvmStatic
        var BROADCAST_PLAY_PLAYLIST_FROM_MAIN = "BROADCAST_PLAY_PLAYLIST_FROM_MAIN"
        @JvmStatic
        var EXTRAX_SONG = "EXTRAX_SONG"

        var EXTRAX_PLAYLIST_ITEM_ID = "EXTRAX_PLAYLIST_ITEM_ID"
        var EXTRAX_PLAYLIST_IS_PLAYING = "EXTRAX_PLAYLIST_IS_PLAYING"
        var EXTRAX_HIDDEN_CONTROLLER = "EXTRAX_HIDDEN_CONTROLLER"
        var PREF_DEFAUT_DESCRIPTION_JSON = "PREF_DEFAUT_DESCRIPTION_JSON"
        var PREF_DEFAUT_PLAYLIST_JSON = "PREF_DEFAUT_PLAYLIST_JSON"
        var PREF_DEFAUT_PLAYLIST_CONTENT = "PREF_DEFAUT_PLAYLIST_CONTENT"
        var PREF_VERSION_DEFAUT_PLAYLIST_JSON = "PREF_VERSION_DEFAUT_PLAYLIST_JSON"
        var PREF_VERSION_APP = "PREF_VERSION_APP"

        //        var DEFAULT_DATA_FOLDER = ".QuantumFrequencies"
        @JvmField
        var DEFAULT_DATA_FOLDER_OLDS = arrayListOf(".QuantumFrequenciesV2", ".QuantumFrequenciesAdvanced")

        @JvmField
        var DEFAULT_DATA_FOLDER = ".QuantumConsoleFrequenciesV3"
        @JvmField
        var DEFAULT_DATA_FOLDER_HIGHER = ".QuantumConsoleFrequenciesHigherV3"
        @JvmField
        var DEFAULT_DATA_FOLDER_INNER = ".QuantumConsoleFrequenciesInnerV3"

        @JvmField
        var CURRENT_PATH_DATA_FOLDER = "CURRENT_PATH_DATA_FOLDER"
        @JvmField
        var DEFAULT_DATA_ADVANCED_FOLDER = ".QuantumFrequenciesAdvancedV3"
        @JvmField
        var DEFAULT_DATA_ABUNDANCE_FOLDER = ".QuantumFrequenciesHigherAbundanceV4"
        @JvmField
        var DEFAULT_DATA_HIGHER_QUANTUM_FOLDER = ".QuantumFrequenciesHigherQuantumV4"

        @JvmField
        var TYPE_ALBUM_BASIC = "TYPE_ALBUM_BASIC"

        @JvmField
        var TYPE_ALBUM_ADVANCED = "TYPE_ALBUM_ADVANCED"

        @JvmStatic
//        val DEFAULT_APKS_FOLDER = ".QuantumFrequenciesAPKs"
        val DEFAULT_APKS_FOLDER = ".QuantumFrequenciesAPKsV2"

        @JvmStatic
        val APKS_FOLDER = "apksFolder"

        @JvmStatic
        val ALBUM_ART_FILE_NAME = "album_art_new_"

        @JvmStatic
        var BROADCAST_PLAY_PLAYLIST = "BROADCAST_PLAY_PLAYLIST"

        val base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgEf2JSlRmK+lZOJBYjQ4fam4F3leuOpwV3v4Iy1idM457raXdJDmI8WNvV2toC7zqPk1QZ6rRS6anOZy1fEOHPUe/lirchT3qovSSVOVdG1t/LhNOQcriikPTNhK5GjNc/kvyBb0+R7u6x/fW/H6MimOIDbjzT5EutWnuRCKHfVg/MgAPWAgsyB8CMUGHSqKNxC5T+zaSHIZSm9U3ociUbZGIpxyJ/bjRCfjmR9Gf98cBdl2CjzrDaGxQP3DD/62vZ+5Hje7AWn3jUg0BGkZYWEbizzQNtwANXOddtCnBU8Kw0YY1v48oakIO3K87ZRYmVQvu9EnTEDhi4nWg5FwhwIDAQAB"

//                val SKU_RIFE_FREE = "com.meditation.sounds.frequencies.7_days_trial"
//                val SKU_RIFE_MONTHLY = "com.meditation.sounds.frequencies.monthly"
//        val SKU_RIFE_YEARLY = "com.meditation.sounds.frequencies.yearly"
//        val SKU_RIFE_FREE_FLASHSALE = "com.meditation.sounds.frequencies.flashsale.7_days_trial"
//                val SKU_RIFE_MONTHLY_FLASHSALE = "com.meditation.sounds.frequencies.flashsale.monthly"
//        val SKU_RIFE_YEARLY_FLASHSALE = "com.meditation.sounds.frequencies.flashsale.yearly"
        val SKU_RIFE_FREE = "com.meditation.sounds.frequencies.7_days_trial"
        val SKU_RIFE_FREE_FLASHSALE = "com.meditation.sounds.frequencies.flashsale.7_days_trial"

        val SKU_RIFE_MONTHLY_FLASHSALE = "com.meditation.sounds.frequencies.flashsale.monthly"
        val SKU_RIFE_YEARLY = "com.meditation.sounds.frequencies.yearly"

        val SKU_RIFE_LIFETIME = "com.meditation.sounds.frequencies.lifetime"
        val SKU_RIFE_LIFETIME_FLASHSALE = "com.meditation.sounds.frequencies.flashsale.lifetime"
        @JvmField
        var KEY_PURCHASED = "KEY_PURCHASED"
        @JvmField
        var KEY_PURCHASED_DEVICE = "KEY_PURCHASED_DEVICE"
        var BROADCAST_ACTION_PURCHASED = "com.zappkit.zappid.BROADCAST_ACTION_PURCHASED"
        var BROADCAST_SHOW_DIALOG_INAPP = "com.zappkit.zappid.BROADCAST_SHOW_DIALOG_INAPP"


        @JvmField
        val PRICE_7_DAY_TRIAL = "PRICE_7_DAY_TRIAL"
        @JvmField
        val PRICE_1_MONTH = "PRICE_1_MONTH"
        @JvmField
        val PRICE_1_YEAR = "PRICE_1_YEAR"
        @JvmField
        val PRICE_LIFETIME = "PRICE_LIFETIME"
        @JvmField
        val PRICE_7_DAY_TRIAL_FLASH_SALE = "PRICE_7_DAY_TRIAL_FLASH_SALE"
        @JvmField
        val PRICE_1_MONTH_FLASH_SALE = "PRICE_1_MONTH_FLASH_SALE"
        @JvmField
        val PRICE_1_YEAR_FLASH_SALE = "PRICE_1_YEAR_FLASH_SALE"
        @JvmField
        val PRICE_LIFETIME_FLASH_SALE = "PRICE_LIFETIME_FLASH_SALE"

        @JvmField
        val INApp_PACKED = "INApp_PACKED"

        @JvmField
        val INApp_FREE = 1

        @JvmField
        val INApp_1_MONTH_25 = 2

        @JvmField
        val INApp_1_MONTH_99 = 3

        @JvmField
        val INApp_LIFETIME = 4

        val SKU_RIFE_MONTHLY = "com.meditation.sounds.frequencies.monthly"
        val SKU_RIFE_ADVANCED_MONTHLY = "com.meditation.sounds.frequencies.monthly_advanced"
        val SKU_RIFE_HIGHER_MONTHLY = "com.meditation.sounds.frequencies.monthly_higherabundance"

        val SKU_RIFE_YEARLY_FLASHSALE = "com.meditation.sounds.frequencies.flashsale.yearly"
        val SKU_RIFE_ADVANCED_YEAR_FLASHSALE = "com.meditation.sounds.frequencies.flashsale.yearly_advanced"
        val SKU_RIFE_HIGHER_ANNUAL_FLASH_SALE = "com.meditation.sounds.frequencies.flashsale.yearly_higherabundance"


        /*advanced*/
        val SKU_RIFE_ADVANCED_FREE_TRIAL = "com.meditation.sounds.frequencies.7_days_trial_advanced"

        val SKU_RIFE_ADVANCED_YEAR = "com.meditation.sounds.frequencies.yearly_advanced"
        val SKU_RIFE_ADVANCED_LIFETIME = "com.meditation.sounds.frequencies.lifetime_advanced"
        val SKU_RIFE_ADVANCED_FREE_TRIAL_FLASHSALE = "com.meditation.sounds.frequencies.flashsale.7_days_trial_advanced"
        val SKU_RIFE_ADVANCED_MONTHLY_FLASHSALE = "com.meditation.sounds.frequencies.flashsale.monthly_advanced"

        val SKU_RIFE_ADVANCED_LIFETIME_FLASHSALE = "com.meditation.sounds.frequencies.flashsale.lifetime_advanced"
        @JvmField
        var KEY_PURCHASED_HIGH_ABUNDANCE = "KEY_PURCHASED_HIGH_ABUNDANCE"
        @JvmField
        var KEY_PURCHASED_HIGH_QUANTUM = "KEY_PURCHASED_HIGH_QUANTUM"
        @JvmField
        var KEY_PURCHASED_ADVANCED = "KEY_PURCHASED_ADVANCED"
        @JvmField
        var KEY_PURCHASED_ADVANCED_DEVICE = "KEY_PURCHASED_ADVANCED_DEVICE"
        @JvmField
        var KEY_PURCHASED_HIGHER_DEVICE = "KEY_PURCHASED_HIGHER_DEVICE"
        @JvmField
        var KEY_PURCHASED_HIGHER_QUANTUM_DEVICE = "KEY_PURCHASED_HIGHER_QUANTUM_DEVICE"

        var BROADCAST_ACTION_PURCHASED_ADVANCED = "com.zappkit.zappid.BROADCAST_ACTION_PURCHASED_ADVANCED"
        @JvmField
        val PRICE_ADVANCED_FREE_TRIAL = "PRICE_ADVANCED_FREE_TRIAL"
        @JvmField
        val PRICE_ADVANCED_MONTHLY = "PRICE_ADVANCED_MONTHLY"
        @JvmField
        val PRICE_ADVANCED_YEAR = "PRICE_ADVANCED_YEAR"
        @JvmField
        val PRICE_ADVANCED_LIFETIME = "PRICE_ADVANCED_LIFETIME"
        @JvmField
        val PRICE_ADVANCED_FREE_TRIAL_FLASH_SALE = "PRICE_ADVANCED_FREE_TRIAL_FLASH_SALE"
        @JvmField
        val PRICE_ADVANCED_MONTHLY_FLASH_SALE = "PRICE_ADVANCED_MONTHLY_FLASH_SALE"
        @JvmField
        val PRICE_ADVANCED_YEAR_FLASH_SALE = "PRICE_ADVANCED_YEAR_FLASH_SALE"
        @JvmField
        val PRICE_ADVANCED_LIFETIME_FLASH_SALE = "PRICE_ADVANCED_LIFETIME_FLASH_SALE"
        @JvmField
        val INApp_ADVANCED_PACKED = "INApp_ADVANCED_PACKED"
        @JvmField
        val INApp_ADVANCED_FREE_TRIAL = 1
        @JvmField
        val INApp_ADVANCED_MONTHLY = 2
        @JvmField
        val INApp_ADVANCED_YEAR = 3
        @JvmField
        val INApp_ADVANCED_LIFETIME = 4

        //HIGHER

        val SKU_RIFE_HIGHER_ANNUAL = "com.meditation.sounds.frequencies.yearly_higherabundance"
        val SKU_RIFE_HIGHER_LIFETIME = "com.meditation.sounds.frequencies.lifetime_higherabundance"

        val SKU_RIFE_HIGHER_LIFETIME_FLASH_SALE = "com.meditation.sounds.frequencies.flashsale.lifetime_higherabundance"
        @JvmField
        val PRICE_HIGHER_MONTHLY = "PRICE_HIGHER_MONTHLY"
        @JvmField
        val PRICE_HIGHER_ANNUAL = "PRICE_HIGHER_ANNUAL"
        @JvmField
        val PRICE_HIGHER_LIFETIME = "PRICE_HIGHER_LIFETIME"
        @JvmField
        val PRICE_HIGHER_ANNUAL_FLASH_SALE = "PRICE_HIGHER_ANNUAL_FLASH_SALE"
        @JvmField
        val PRICE_HIGHER_LIFETIME_FLASH_SALE = "PRICE_HIGHER_LIFETIME_FLASH_SALE"
        @JvmField
        val INApp_HIGHER_PACKED = "INApp_HIGHER_PACKED"
        @JvmField
        val INApp_HIGHER_FREE_TRIAL = 1
        @JvmField
        val INApp_HIGHER_MONTHLY = 2
        @JvmField
        val INApp_HIGHER_YEAR = 3
        @JvmField
        val INApp_HIGHER_LIFETIME = 4
        /**/

        @JvmField
        val MEDIA_TYPE_NONE = 0
        @JvmField
        val MEDIA_TYPE_BASIC_FREE = 1
        @JvmField
        val MEDIA_TYPE_BASIC = 2
        @JvmField
        val MEDIA_TYPE_ADVANCED = 3
        @JvmField
        val MEDIA_TYPE_ABUNDANCE = 4
        @JvmField
        val MEDIA_TYPE_HIGHER_QUANTUM = 5

        @JvmField
        val PREF_FLASH_SALE = "PREF_FLASH_SALE"
        @JvmField
        val PREF_FLASH_SALE_COUNTERED = "PREF_FLASH_SALE_COUNTERED"

        @JvmField
        val ETRAX_FLASH_SALE_INIT = 1
        const val ETRAX_FLASH_SALE_FIRST_NOTIFICATION = 2
        const val ETRAX_FLASH_SALE_SECOND_NOTIFICATION = 3
        const val ETRAX_FLASH_SALE_THIRD_NOTIFICATION = 4

        @JvmField
        val ETRAX_REMINDER_NOTIFICATION = 5
        @JvmField
        val FREF_REMINDER_NOTIFICATION_ITEM_POSITION = "REMINDER_NOTIFICATION_ITEM_POSITION"

        @JvmField
        val REMINDER_NOTIFICATION_ID = 1989

        @JvmField
        val ETRAX_FLASH_SALE_TYPE = "ETRAX_FLASH_SALE_TYPE"

        @JvmField
        val ETRAX_FIRST_INSTALLER_APP_TIME = "ETRAX_FIRST_INSTALLER_APP_TIME"

        @JvmField
        val ACTION_RECEIVE_FLASHSALE_NOTIFICATION = "com.Meditation.Sounds.frequencies.ACTION_RECEIVE_FLASHSALE_NOTIFICATION"

        @JvmField
        val ACTION_RE_DOWNLOAD_MP3 = "com.Meditation.Sounds.frequencies.ACTION_RE_DOWNLOAD_MP3"

        @JvmField
        val API_KEY = "3I8ZiXe7SWlfzjvf8on0"

        @JvmField
        internal var PREF_PROFILE = "PREF_PROFILE"
        @JvmField
        internal var PREF_EMAIL = "PREF_EMAIL"
        @JvmField
        internal var PREF_PASSWORD = "PREF_PASSWORD"
        @JvmField
        internal var IS_PREMIUM = "IS_PREMIUM"
        @JvmField
        internal var IS_MASTER = "IS_MASTER"
        @JvmField
        internal var IS_UNLOCK_ALL = "IS_UNLOCK_ALL"
        @JvmField
        internal var CODE_SUCCESS = "CODE_SUCCESS"

        @JvmField
        var KEY_DEVICE_ID = "KEY_DEVICE_ID"
        @JvmField
        var EXTENSION_ENCRYPT_FILE = "shapee"
        @JvmField
        var EXTENSION_MP3_FILE = "mp3"
        @JvmStatic
        val ALBUM_INFOR_FILE_NAME = "album_infor.json"
        @JvmStatic
        val IS_DOWNLOADED_ALL_ALBUM = "IS_DOWNLOADED_ALL_ALBUM"

        @JvmStatic
        var CURRENT_VOLUME = -1

//        var tracks = ArrayList<Track>()

        var isGuestLogin = false
    }
}
