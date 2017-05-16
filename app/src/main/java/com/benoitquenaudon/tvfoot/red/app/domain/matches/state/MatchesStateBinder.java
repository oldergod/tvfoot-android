package com.benoitquenaudon.tvfoot.red.app.domain.matches.state;

import android.os.Bundle;
import com.benoitquenaudon.tvfoot.red.app.common.schedulers.BaseSchedulerProvider;
import com.benoitquenaudon.tvfoot.red.app.data.entity.Match;
import com.benoitquenaudon.tvfoot.red.app.domain.match.MatchDisplayable;
import com.benoitquenaudon.tvfoot.red.app.injection.scope.ScreenScope;
import com.google.firebase.analytics.FirebaseAnalytics;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.functions.BiFunction;
import io.reactivex.subjects.PublishSubject;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import timber.log.Timber;

import static com.benoitquenaudon.tvfoot.red.app.common.PreConditions.checkNotNull;

@ScreenScope public class MatchesStateBinder {
  private PublishSubject<MatchesIntent> intentsSubject;
  private PublishSubject<MatchesViewState> statesSubject;
  private MatchesService service;
  private BaseSchedulerProvider schedulerProvider;
  private FirebaseAnalytics firebaseAnalytics;

  @SuppressWarnings("CheckReturnValue") //
  @Inject MatchesStateBinder(PublishSubject<MatchesIntent> intentsSubject,
      PublishSubject<MatchesViewState> statesSubject, MatchesService service,
      BaseSchedulerProvider schedulerProvider, FirebaseAnalytics firebaseAnalytics) {
    this.intentsSubject = intentsSubject;
    this.statesSubject = statesSubject;
    this.service = service;
    this.schedulerProvider = schedulerProvider;
    this.firebaseAnalytics = firebaseAnalytics;

    compose().subscribe(state -> this.statesSubject.onNext(state));
  }

  @SuppressWarnings("CheckReturnValue")
  public void forwardIntents(Observable<MatchesIntent> intents) {
    intents.subscribe(intentsSubject::onNext);
  }

  public Observable<MatchesViewState> getStatesAsObservable() {
    return statesSubject;
  }

  private Observable<MatchesViewState> compose() {
    return intentsSubject.doOnNext(this::logIntent)
        .scan(initialIntentFilter)
        .map(this::actionFromIntent)
        .doOnNext(this::logAction)
        .compose(actionToResultTransformer)
        .doOnNext(this::logResult)
        .scan(MatchesViewState.idle(), reducer)
        .doOnNext(this::logState);
  }

  private BiFunction<MatchesIntent, MatchesIntent, MatchesIntent> initialIntentFilter =
      (previousIntent, newIntent) -> {
        // if isReConnection (e.g. after config change)
        // i.e. we are inside the scan, meaning there has already
        // been intent in the past, meaning the InitialIntent cannot
        // be the first => it is a reconnection.
        if (newIntent instanceof MatchesIntent.InitialIntent) {
          return MatchesIntent.GetLastState.create();
        } else {
          return newIntent;
        }
      };

  private MatchesAction actionFromIntent(MatchesIntent intent) {
    if (intent instanceof MatchesIntent.InitialIntent) {
      return MatchesAction.RefreshAction.create();
    }
    if (intent instanceof MatchesIntent.RefreshIntent) {
      return MatchesAction.RefreshAction.create();
    }
    if (intent instanceof MatchesIntent.GetLastState) {
      return MatchesAction.GetLastStateAction.create();
    }
    if (intent instanceof MatchesIntent.LoadNextPageIntent) {
      return MatchesAction.LoadNextPageAction.create(
          ((MatchesIntent.LoadNextPageIntent) intent).pageIndex());
    }
    throw new IllegalArgumentException("do not know how to treat this intents " + intent);
  }

  private ObservableTransformer<MatchesAction.RefreshAction, MatchesResult.RefreshResult>
      refreshTransformer = actions -> actions.flatMap(action -> service.loadPage(0)
      .toObservable()
      .map(MatchesResult.RefreshResult::success)
      .onErrorReturn(MatchesResult.RefreshResult::failure)
      .subscribeOn(schedulerProvider.io())
      .observeOn(schedulerProvider.ui())
      .startWith(MatchesResult.RefreshResult.inFlight()));

  private ObservableTransformer<MatchesAction.LoadNextPageAction, MatchesResult.LoadNextPageResult>
      loadNextPageTransformer = actions -> actions.flatMap(
      action -> service.loadPage(action.pageIndex())
          .toObservable()
          .map(MatchesResult.LoadNextPageResult::success)
          .onErrorReturn(MatchesResult.LoadNextPageResult::failure)
          .subscribeOn(schedulerProvider.io())
          .observeOn(schedulerProvider.ui())
          .startWith(MatchesResult.LoadNextPageResult.inFlight(action.pageIndex())));

  private ObservableTransformer<MatchesAction.GetLastStateAction, MatchesResult.GetLastStateResult>
      getLastStateTransformer =
      actions -> actions.map(ignored -> MatchesResult.GetLastStateResult.create());

  private ObservableTransformer<MatchesAction, MatchesResult> actionToResultTransformer =
      actions -> actions.publish(shared -> Observable.merge(
          shared.ofType(MatchesAction.RefreshAction.class).compose(refreshTransformer),
          shared.ofType(MatchesAction.LoadNextPageAction.class).compose(loadNextPageTransformer),
          shared.ofType(MatchesAction.GetLastStateAction.class).compose(getLastStateTransformer))
          .mergeWith(
              // Error for not implemented actions
              shared.filter(v -> !(v instanceof MatchesAction.RefreshAction)
                  && !(v instanceof MatchesAction.LoadNextPageAction)
                  && !(v instanceof MatchesAction.GetLastStateAction))
                  .flatMap(w -> Observable.error(
                      new IllegalArgumentException("Unknown Action type: " + w)))));

  private static BiFunction<MatchesViewState, MatchesResult, MatchesViewState> reducer =
      (previousState, matchesResult) -> {
        MatchesViewState.Builder stateBuilder = previousState.buildWith();

        // check matchesResult and update state accordingly
        if (matchesResult instanceof MatchesResult.RefreshResult) {
          switch (((MatchesResult.RefreshResult) matchesResult).status()) {
            case REFRESH_IN_FLIGHT:
              stateBuilder.refreshLoading(true).error(null);
              break;
            case REFRESH_FAILURE:
              stateBuilder.refreshLoading(false)
                  .error(((MatchesResult.RefreshResult) matchesResult).throwable());
              break;
            case REFRESH_SUCCESS:
              List<Match> matches =
                  checkNotNull(((MatchesResult.RefreshResult) matchesResult).matches(),
                      "Matches are null");

              stateBuilder.refreshLoading(false)
                  .error(null)
                  .currentPage(0)
                  .matches(MatchDisplayable.fromMatches(matches));
              break;
            default:
              throw new IllegalArgumentException(
                  "Wrong status for RefreshResult: " + ((MatchesResult.RefreshResult) matchesResult)
                      .status());
          }
        } else if (matchesResult instanceof MatchesResult.LoadNextPageResult) {
          switch (((MatchesResult.LoadNextPageResult) matchesResult).status()) {
            case NEXT_PAGE_IN_FLIGHT:
              stateBuilder.nextPageLoading(true)
                  .currentPage(((MatchesResult.LoadNextPageResult) matchesResult).pageIndex())
                  .error(null);
              break;
            case NEXT_PAGE_FAILURE:
              stateBuilder.nextPageLoading(false)
                  .error(((MatchesResult.LoadNextPageResult) matchesResult).error());
              break;
            case NEXT_PAGE_SUCCESS:
              List<Match> newMatches =
                  checkNotNull(((MatchesResult.LoadNextPageResult) matchesResult).matches(),
                      "Matches are null");

              List<MatchDisplayable> matches = new ArrayList<>();
              matches.addAll(previousState.matches());
              matches.addAll(MatchDisplayable.fromMatches(newMatches));

              stateBuilder.nextPageLoading(false).error(null).matches(matches);
              break;
            default:
              throw new IllegalArgumentException("Wrong status for LoadNextPageResult: "
                  + ((MatchesResult.LoadNextPageResult) matchesResult).status());
          }
        } else if (matchesResult instanceof MatchesResult.GetLastStateResult) {
          return stateBuilder.build();
        } else {
          throw new IllegalArgumentException("Don't know this matchesResult " + matchesResult);
        }

        return stateBuilder.build();
      };

  private void logIntent(MatchesIntent intent) {
    Timber.d("Intent: %s", intent);
    Bundle params = new Bundle();
    params.putString("intent", intent.toString());
    firebaseAnalytics.logEvent("intent", params);
  }

  private void logAction(MatchesAction action) {
    Timber.d("Action: %s", action);
    Bundle params = new Bundle();
    params.putString("action", action.toString());
    firebaseAnalytics.logEvent("action", params);
  }

  private void logResult(MatchesResult result) {
    Timber.d("Rntent: %s", result);
    Bundle params = new Bundle();
    params.putString("result", result.toString());
    firebaseAnalytics.logEvent("result", params);
  }

  private void logState(MatchesViewState state) {
    Timber.d("Sntent: %s", state);
    Bundle params = new Bundle();
    params.putString("state", state.toString());
    firebaseAnalytics.logEvent("state", params);
  }
}
