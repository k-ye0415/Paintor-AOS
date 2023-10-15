package com.jina.paintor.activity

import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.view.children
import com.jina.paintor.R
import com.jina.paintor.adapter.DayViewContainer
import com.jina.paintor.calendar.ContinuousSelectionHelper
import com.jina.paintor.calendar.ContinuousSelectionHelper.getSelection
import com.jina.paintor.calendar.DateSelection
import com.jina.paintor.databinding.ActivityCalendarBinding
import com.jina.paintor.databinding.IncludeDayOfWeekBinding
import com.jina.paintor.databinding.ItemCalendarDayBinding
import com.jina.paintor.utils.TAG
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.yearMonth
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.orhanobut.logger.Logger
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class CalendarActivity : AppCompatActivity() {

    private val binding: ActivityCalendarBinding by lazy {
        ActivityCalendarBinding.inflate(layoutInflater)
    }
    private var selectDate = DateSelection()
    private var today = LocalDate.now()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.includeToolbar.toolbarTitle = "여행 계획"
        binding.includeToolbar.ivNewPlan.visibility = View.VISIBLE
        setCalendarView()

        binding.tvToday.setOnClickListener {
            binding.calendarView.smoothScrollToMonth(YearMonth.now())
        }
    }

    fun setCalendarView() {
        val daysOfWeek = daysOfWeek()
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(100)
        val endMonth = currentMonth.plusMonths(100)

        class DayViewContainer(view: View) : ViewContainer(view) {
            lateinit var day: CalendarDay // Will be set when this container is bound.
            val binding = ItemCalendarDayBinding.bind(view)

            init {
                view.setOnClickListener {
                    if (day.position == DayPosition.MonthDate &&
                        (day.date == today || day.date.isAfter(today))
                    ) {
                        selectDate = getSelection(
                            clickedDate = day.date,
                            dateSelection = selectDate,
                        )
                        this@CalendarActivity.binding.calendarView.notifyCalendarChanged()
                    }
                }
            }
        }

        binding.layoutDayOfWeek.root.children.map { it as TextView }
            .forEachIndexed { index, textView ->
                val dayOfWeek = daysOfWeek[index]
                val title = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                textView.text = title
            }

        binding.calendarView.apply {
            dayBinder = object : MonthDayBinder<DayViewContainer> {
                override fun create(view: View) = DayViewContainer(view)
                override fun bind(container: DayViewContainer, data: CalendarDay) {
                    container.day = data
                    bindDate(data, container.binding.tvDay, container.binding.viewRange)
                    val isSunday = data.date.dayOfWeek == DayOfWeek.SUNDAY
                    val isSaturday = data.date.dayOfWeek == DayOfWeek.SATURDAY
                    container.binding.tvDay.setTextColor(
                        when {
                            isSunday -> {
                                Color.RED
                            }

                            isSaturday -> {
                                Color.BLUE
                            }

                            else -> {
                                Color.BLACK
                            }
                        }
                    )
                }
            }
            monthScrollListener = { updateTitle() }
            setup(startMonth, endMonth, daysOfWeek().first())
            scrollToMonth(currentMonth)
        }
    }

    fun bindDate(data: CalendarDay, textView: TextView, viewRange: View) {
        val (startDate, endDate) = selectDate
        when (data.position) {
            DayPosition.MonthDate -> {
                textView.text = data.date.dayOfMonth.toString()
                when {
                    // Start Date 를 클릭 시 해당 스코프를 진행
                    startDate == data.date && endDate == null -> {
                        Logger.t(TAG.CALENDAR).i("Start Date ${data.date}")
                        textView.setTextColor(Color.WHITE)
                        textView.setBackgroundResource(R.drawable.shape_bg_single_day)
                    }
                    // End Date 를 클릭 시 해당 스코프를 진행
                    data.date == startDate -> {
                        Logger.t(TAG.CALENDAR).i("Start Date 인데 endDate 클릭 시 지정 됨 ${data.date}")
                        textView.setBackgroundResource(R.drawable.shape_bg_single_day)
                        viewRange.visibility = View.VISIBLE
                        viewRange.setBackgroundResource(R.drawable.shape_bg_start_day)
                    }
                    // Range Date
                    startDate != null && endDate != null && (data.date > startDate && data.date < endDate) -> {
                        Logger.t(TAG.CALENDAR).d("Range Date : ${data.date}")
                        textView.setTextColor(Color.DKGRAY)
                        viewRange.visibility = View.VISIBLE
                        viewRange.setBackgroundResource(R.drawable.shape_bg_range_day)
                    }
// end date 클릭
                    data.date == endDate -> {
                        Logger.t(TAG.CALENDAR).i("endDate : ${data.date}")
                        textView.setTextColor(getColor(R.color.white))
                        textView.setBackgroundResource(R.drawable.shape_bg_single_day)
                        viewRange.visibility = View.VISIBLE
                        viewRange.setBackgroundResource(R.drawable.shape_bg_end_day)
                    }

                    data.date == today -> {
                        Logger.t(TAG.CALENDAR).d("today : ${data.date}")
                        textView.setTextColor(getColor(R.color.main_color))
                    }

                    else -> {
                        textView.setTextColor(getColor(R.color.black))
                        textView.background = null
                        viewRange.background = null
                    }
                }
            }

            DayPosition.InDate -> {
                if (startDate != null && endDate != null && ContinuousSelectionHelper.isInDateBetweenSelection(
                        data.date,
                        startDate,
                        endDate
                    )
                ) {
                    Logger.t(TAG.CALENDAR).d("InDate : ${data.date}")
                    viewRange.setBackgroundResource(R.drawable.shape_bg_single_day)
                }
            }

            DayPosition.OutDate -> {
                if (startDate != null && endDate != null && ContinuousSelectionHelper.isOutDateBetweenSelection(
                        data.date,
                        startDate,
                        endDate
                    )
                ) {
                    Logger.t(TAG.CALENDAR).d("OutDate : ${data.date}")
                    viewRange.setBackgroundResource(R.drawable.shape_bg_single_day)
                }
            }
        }
    }

    private fun updateTitle() {
        val month = binding.calendarView.findFirstVisibleMonth()?.yearMonth ?: return
        binding.tvMonth.text = month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }
}