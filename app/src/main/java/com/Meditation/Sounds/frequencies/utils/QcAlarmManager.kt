package com.Meditation.Sounds.frequencies.utils

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.Meditation.Sounds.frequencies.api.models.GetFlashSaleOutput
import com.Meditation.Sounds.frequencies.services.AlarmReceiver
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

class QcAlarmManager{
    companion object {
        var countAlarm = 0

        @JvmStatic
        fun createAlarms(context: Context) {
            if(SharedPreferenceHelper.getInstance().getBool(Constants.KEY_PURCHASED) && SharedPreferenceHelper.getInstance().getBool(Constants.KEY_PURCHASED_ADVANCED)){
                clearAlarms(context)
                return
            }
            var jsonFlashSale = SharedPreferenceHelper.getInstance().get(Constants.PREF_FLASH_SALE)
            var flashsale = Gson().fromJson(jsonFlashSale, GetFlashSaleOutput::class.java)
            clearAlarms(context)
            if (flashsale.flashSale != null) {
                if (flashsale.flashSale.enable!!) {

                    var initDelay = flashsale.flashSale.initDelay
                    var duration = flashsale.flashSale.duration
                    var interval = flashsale.flashSale.interval
//                var interval = flashsale.flashSale.initDelay

                    var currentCal = Calendar.getInstance()
                    var fistIntallerAppTime = SharedPreferenceHelper.getInstance().getLong(Constants.ETRAX_FIRST_INSTALLER_APP_TIME)
                    var initFSTime = fistIntallerAppTime + (initDelay!! * 60 * 60 * 1000).toLong()
                    var calInitFSTime = Calendar.getInstance()
                    calInitFSTime.timeInMillis = initFSTime
                    createNewAlarms(context, currentCal, calInitFSTime, interval!!, Constants.ETRAX_FLASH_SALE_INIT)

                    if (flashsale.flashSale.ntf != null) {
                        if (flashsale.flashSale.ntf!!.first != null) {
                            var firstFlashSale = initFSTime + (flashsale.flashSale.ntf!!.first!!.delay!! * 60 * 60 * 1000).toLong()
//                        var firstFlashSale = initFSTime + (60 * 1000).toLong()
                            var calFirstFS = Calendar.getInstance()
                            calFirstFS.timeInMillis = firstFlashSale
                            createNewAlarms(context, currentCal, calFirstFS, interval!!, Constants.ETRAX_FLASH_SALE_FIRST_NOTIFICATION)
                        }

                        if (flashsale.flashSale.ntf!!.second != null) {
                            var secondFlashSale = initFSTime + (flashsale.flashSale.ntf!!.second!!.delay!! * 60 * 60 * 1000).toLong()
//                        var secondFlashSale = initFSTime + (2 * 60 * 1000).toLong()
                            var calSecondFS = Calendar.getInstance()
                            calSecondFS.timeInMillis = secondFlashSale
                            createNewAlarms(context, currentCal, calSecondFS, interval!!, Constants.ETRAX_FLASH_SALE_SECOND_NOTIFICATION)
                        }

                        if (flashsale.flashSale.ntf!!.third != null) {
                            var thirdFlashSale = initFSTime + (flashsale.flashSale.ntf!!.third!!.delay!! * 60 * 60 * 1000).toLong()
//                        var thirdFlashSale = initFSTime + (3 * 60 * 1000).toLong()
                            var calThirdFS = Calendar.getInstance()
                            calThirdFS.timeInMillis = thirdFlashSale
                            createNewAlarms(context, currentCal, calThirdFS, interval!!, Constants.ETRAX_FLASH_SALE_THIRD_NOTIFICATION)
                        }
                    }
                } else {
                    QcAlarmManager.clearAlarms(context)
                }
            } else {
                QcAlarmManager.clearAlarms(context)
            }
        }

        @RequiresApi(Build.VERSION_CODES.M)
        @JvmStatic
        fun createNewAlarms(context: Context, currentCal: Calendar, alarmCal: Calendar, interval: Float, flashSaleType: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            while (alarmCal.before(currentCal)) {
                alarmCal.add(Calendar.SECOND, (interval!! * 24 * 60 * 60).toInt())
//            alarmCal.add(Calendar.SECOND, (5 * 60).toInt())
            }

            val intent = Intent(context, AlarmReceiver::class.java)
            intent.putExtra(Constants.ETRAX_FLASH_SALE_TYPE, flashSaleType)
            if (countAlarm > 20) {
                countAlarm = 0
            }
            countAlarm++

            var dateFormat = SimpleDateFormat("hh::mm:ss")
            Log.d("MENDATE", ""  + flashSaleType + "-" + dateFormat.format(alarmCal.time))
            val pendingIntent = PendingIntent.getBroadcast(context, countAlarm, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmCal.timeInMillis, pendingIntent)
            } else {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, alarmCal.timeInMillis, (interval!! * 24 * 60 * 60 * 1000).toLong(), pendingIntent)
                //        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, alarmCal.timeInMillis, (5 * 60 * 1000).toLong(), pendingIntent)
            }
        }

        @JvmStatic
        fun clearAlarms(context: Context) {
            for (i in 0..21) {
                val intent = Intent(context, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(context, i, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(pendingIntent)
            }
        }

        @JvmStatic
        fun clearReminderAlarm(context: Context){

        }

        @SuppressLint("UnspecifiedImmutableFlag")
        @JvmStatic
        fun createReminderAlarm(context: Context){
            //Remove
            val intent = Intent(context, AlarmReceiver::class.java)
            intent.putExtra(Constants.ETRAX_FLASH_SALE_TYPE, Constants.ETRAX_REMINDER_NOTIFICATION)
            val pendingIntent = PendingIntent.getBroadcast(context, Constants.REMINDER_NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            //Re-create reminder
            val jsonFlashSale = SharedPreferenceHelper.getInstance().get(Constants.PREF_FLASH_SALE)
            if(jsonFlashSale != null) {
                var flashsale = Gson().fromJson(jsonFlashSale, GetFlashSaleOutput::class.java)
                if (flashsale?.reminder != null && flashsale.reminder.messages != null && flashsale.reminder.messages!!.size > 0){
                    val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val date = hourFormat.parse(flashsale.reminder.launchTime)

                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.HOUR_OF_DAY, date.hours)
                    calendar.set(Calendar.MINUTE, date.minutes)
                    calendar.set(Calendar.SECOND, 0)
                    val currentCalender = Calendar.getInstance()
                    if(calendar.timeInMillis < currentCalender.timeInMillis){
                        calendar.add(Calendar.DATE, 1)
                        calendar.set(Calendar.HOUR_OF_DAY, date.hours)
                        calendar.set(Calendar.MINUTE, date.minutes)
                        calendar.set(Calendar.SECOND, 0)
                    }
//                    intent.putExtra(Constants.ETRAX_FLASH_SALE_TYPE, Constants.ETRAX_REMINDER_NOTIFICATION)

                    val dateFormat = SimpleDateFormat("dd:MM:yyyy HH:mm:ss")
                    Log.d("MENDATE", "Reminder-" + dateFormat.format(calendar.time))

                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, (flashsale.reminder.interval!! * 60 * 60 * 1000).toLong(), pendingIntent)
                }
            }
        }
    }
}