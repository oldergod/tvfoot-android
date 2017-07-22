package com.benoitquenaudon.tvfoot.red.app.domain.matches.displayable

import com.benoitquenaudon.tvfoot.red.R
import com.benoitquenaudon.tvfoot.red.util.isCurrentYear
import com.benoitquenaudon.tvfoot.red.util.isToday
import com.benoitquenaudon.tvfoot.red.util.isTomorrow
import timber.log.Timber
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.LazyThreadSafetyMode.NONE

@Suppress("DataClassPrivateConstructor")
data class HeaderRowDisplayable private constructor(
    val dangerResId: Int,
    val hasDanger: Boolean,
    val displayedDate: String
) : MatchesItemDisplayable {
  override fun isSameAs(newItem: MatchesItemDisplayable): Boolean {
    return newItem is HeaderRowDisplayable && this.displayedDate == newItem.displayedDate
  }

  companion object Factory {
    private val headerKeyDateFormat: DateFormat by lazy(NONE) {
      val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
      format.timeZone = TimeZone.getDefault()
      format
    }
    private val monthDateFormat: DateFormat by lazy(NONE) {
      val format = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
      format.timeZone = TimeZone.getDefault()
      format
    }
    private val yearDateFormat: DateFormat by lazy(NONE) {
      val format = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
      format.timeZone = TimeZone.getDefault()
      format
    }

    fun create(headerKey: String): HeaderRowDisplayable {
      val date: Date
      try {
        date = headerKeyDateFormat.parse(headerKey)
      } catch (e: ParseException) {
        Timber.e(e)
        throw UnsupportedOperationException("What is this date anyway? " + headerKey)
      }

      var dangerResId = -1
      val displayedDate: String
      val nowCalendar = Calendar.getInstance()
      if (date.time.isToday(nowCalendar)) {
        dangerResId = R.string.matches_row_header_danger_today
        displayedDate = monthDateFormat.format(date).capitalize()
        return HeaderRowDisplayable(dangerResId, true, displayedDate)
      }

      if (date.time.isTomorrow(nowCalendar)) {
        dangerResId = R.string.matches_row_header_danger_tomorrow
        displayedDate = monthDateFormat.format(date).capitalize()
        return HeaderRowDisplayable(dangerResId, true, displayedDate)
      }

      if (date.time.isCurrentYear(nowCalendar)) {
        displayedDate = monthDateFormat.format(date).capitalize()
      } else {
        displayedDate = yearDateFormat.format(date)
      }

      return HeaderRowDisplayable(dangerResId, false, displayedDate)
    }
  }
}