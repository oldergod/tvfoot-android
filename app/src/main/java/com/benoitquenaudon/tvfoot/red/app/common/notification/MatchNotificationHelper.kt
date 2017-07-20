package com.benoitquenaudon.tvfoot.red.app.common.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import com.benoitquenaudon.tvfoot.red.R
import com.benoitquenaudon.tvfoot.red.app.data.entity.Match
import com.benoitquenaudon.tvfoot.red.app.data.entity.Match.MATCH_ID
import com.benoitquenaudon.tvfoot.red.app.domain.match.MatchActivity
import com.benoitquenaudon.tvfoot.red.app.domain.match.MatchDisplayable

class MatchNotificationHelper(private val context: Context, private val match: Match) {
  fun publishMatchStarting() {
    val intent = Intent(context, MatchActivity::class.java).also {
      it.putExtra(MATCH_ID, match.id())
    }

    val stackBuilder = TaskStackBuilder.create(context).also {
      it.addParentStack(MatchActivity::class.java)
      it.addNextIntent(intent)
    }

    val pendingIntent = stackBuilder
        .getPendingIntent(NotificationService.matchIdAsInt(match.id()),
            PendingIntent.FLAG_UPDATE_CURRENT)

    val matchDisplayable = MatchDisplayable.fromMatch(match)

    val notificationBuilder = NotificationCompat.Builder(context)
        .setSmallIcon(R.drawable.logo)
        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .setContentTitle(matchDisplayable.headline())
        .setContentText(matchDisplayable.matchDay())
        .setSubText(matchDisplayable.competition())
        .setWhen(match.startAt().time)

    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(NotificationService.matchIdAsInt(match.id()),
        notificationBuilder.build())
  }
}