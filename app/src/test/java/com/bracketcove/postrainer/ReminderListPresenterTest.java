package com.bracketcove.postrainer;

import com.bracketcove.postrainer.data.alarm.AlarmService;
import com.bracketcove.postrainer.data.reminder.ReminderService;
import com.bracketcove.postrainer.data.viewmodel.Reminder;
import com.bracketcove.postrainer.reminderlist.ReminderListContract;
import com.bracketcove.postrainer.reminderlist.ReminderListPresenter;
import com.bracketcove.postrainer.util.BaseSchedulerProvider;
import com.bracketcove.postrainer.util.SchedulerProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by Ryan on 09/03/2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class ReminderListPresenterTest {

    @Mock
    private ReminderListContract.View view;

    @Mock
    private ReminderService reminderService;

    @Mock
    private AlarmService alarmService;

    private BaseSchedulerProvider schedulerProvider;

    private ReminderListPresenter presenter;

    private static final String TITLE = "Coffee Break";

    private static final int MINUTE = 30;

    private static final int HOUR = 10;

    private static final String DEFAULT_NAME = "New Alarm";

    private static final boolean ALARM_STATE = true;

    //TODO: fix this test data to look the same as implementation would
    private static final String REMINDER_ID = "111111111111111";

    private static final Reminder ACTIVE_REMINDER = new Reminder(
            REMINDER_ID,
            TITLE,
            true,
            false,
            false,
            MINUTE,
            HOUR
    );

    private static final Reminder INACTIVE_REMINDER = new Reminder(
            REMINDER_ID,
            TITLE,
            false,
            false,
            false,
            HOUR,
            MINUTE

    );

    @Before
    public void SetUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        schedulerProvider = new SchedulerProvider();

        presenter = new ReminderListPresenter(
                view,
                reminderService,
                alarmService,
                schedulerProvider
        );
    }

    /**
     * At least one RealmReminder found in storage. Display it/them to user.
     */
    @Test
    public void onGetRemindersNotEmpty() {
        List<Reminder> reminderList = new ArrayList<>();
        reminderList.add(INACTIVE_REMINDER);

        when(reminderService.getReminders()).thenReturn(Observable.just(reminderList));

        presenter.subscribe();

        verify(view).setReminderListData(Mockito.anyList());
    }

    /**
     * No reminders found in storage. Show add reminder prompt.
     */
    @Test
    public void onGetRemindersEmpty() {
        when(reminderService.getReminders()).thenReturn(Observable.<List<Reminder>>empty());

        presenter.subscribe();

        verify(view).setNoReminderListDataFound();
    }

    /**
     * Storage throws an error
     */
    @Test
    public void onGetRemindersError() {
        when(reminderService.getReminders()).thenReturn(
                Observable.<List<Reminder>>error(new Exception())
        );


        presenter.subscribe();

        verify(view).makeToast(R.string.error_database_connection_failure);
    }

    /**
     * Tests be behaviour when:
     * User toggle's RealmReminder Active Switch, and
     * current state of alarm matches is the same.
     * If so, no need to update the Repository
     */
    @Test
    public void onReminderToggledStatesMatchFalse() {
        presenter.onReminderToggled(false, ACTIVE_REMINDER);
        verify(view).makeToast(R.string.msg_alarm_deactivated);
    }

    @Test
    public void onReminderToggledStatesMatchTrue() {
        presenter.onReminderToggled(true, INACTIVE_REMINDER);
        verify(view).makeToast(R.string.msg_alarm_activated);
    }

    /**
     * Tests be behaviour when:
     * User toggle's RealmReminder Active Switch, and
     * current state of alarm matches is different.
     * If so, update Repo accordingly.
     */
    @Test
    public void onReminderToggledStatesDifferActivate() {

        Mockito.when(reminderService.updateReminder(ACTIVE_REMINDER))
                .thenReturn(Observable.empty());

        Mockito.when(alarmService.setAlarm(ACTIVE_REMINDER))
                .thenReturn(Completable.complete());

        presenter.onReminderToggled(true, INACTIVE_REMINDER);

        verify(view).makeToast(R.string.msg_alarm_activated);
    }

    @Test
    public void onReminderToggledStatesDifferDeactivate() {
        Mockito.when(reminderService.updateReminder(INACTIVE_REMINDER))
                .thenReturn(Observable.empty());

        Mockito.when(alarmService.setAlarm(INACTIVE_REMINDER))
                .thenReturn(Completable.complete());

        presenter.onReminderToggled(false, ACTIVE_REMINDER);
        verify(view).makeToast(R.string.msg_alarm_deactivated);
    }


    @Test
    public void onReminderSuccessfullyDeleted() {
        Mockito.when(reminderService.deleteReminder(ACTIVE_REMINDER))
                .thenReturn(Observable.empty());

        presenter.onReminderSwiped(1, ACTIVE_REMINDER);

        verify(view).makeToast(R.string.msg_alarm_deleted);
    }

    @Test
    public void onReminderUnsuccessfullyDeleted() {
        Mockito.when(reminderService.deleteReminder(ACTIVE_REMINDER))
                .thenReturn(Observable.error(new Exception()));

        presenter.onReminderSwiped(1, ACTIVE_REMINDER);

        verify(view).makeToast(R.string.error_database_connection_failure);
        verify(view).undoDeleteReminderAt(1, ACTIVE_REMINDER);
    }

    @Test
    public void onSettingsIconClicked() {
        presenter.onSettingsIconClick();
        verify(view).startSettingsActivity();
    }

    /**
     * Maximum number of Reminders is currently 5. I'm not sure why you'd need more, but hopefully
     * customer feedback will solve this issue.
     */
    @Test
    public void whenUserTriesToAddMoreThanFiveReminders() {
        presenter.onCreateReminderButtonClick(5, DEFAULT_NAME, REMINDER_ID);
         view.makeToast(R.string.msg_reminder_limit_reached);
    }

    /**
     * When we create a RealmReminder, we must add it to storage
     * as well as the View.
     */
    @Test
    public void onNewReminderCreatedSuccessfully() {
        presenter.onCreateReminderButtonClick(1, DEFAULT_NAME, REMINDER_ID);

        verify(view).addNewReminderToListView(Mockito.any(Reminder.class));
    }

    @Test
    public void onNewReminderCreatedUnsuccessfully() {
        presenter.onCreateReminderButtonClick(1, DEFAULT_NAME, REMINDER_ID);

        verify(view).makeToast(R.string.error_database_write_failure);
    }

    /**
     * This means that the user wants to edit a RealmReminder
     */
    @Test
    public void onReminderIconClicked() {
        presenter.onReminderIconClick(ACTIVE_REMINDER);

        verify(view).startReminderDetailActivity(REMINDER_ID);
    }

}