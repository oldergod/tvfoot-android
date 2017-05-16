package com.benoitquenaudon.tvfoot.red.app.domain.match.state;

import com.benoitquenaudon.tvfoot.red.app.data.entity.Match;
import com.benoitquenaudon.tvfoot.red.app.domain.match.MatchDisplayable;
import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

import static com.benoitquenaudon.tvfoot.red.app.domain.match.state.MatchResult.Status.LOAD_MATCH_FAILURE;
import static com.benoitquenaudon.tvfoot.red.app.domain.match.state.MatchResult.Status.LOAD_MATCH_IN_FLIGHT;
import static com.benoitquenaudon.tvfoot.red.app.domain.match.state.MatchResult.Status.LOAD_MATCH_SUCCESS;

interface MatchResult {
  @AutoValue abstract class LoadMatchResult implements MatchResult {
    abstract Status status();

    @Nullable abstract Match match();

    @Nullable abstract Throwable error();

    abstract boolean shouldNotifyMatchStart();

    static LoadMatchResult success(Match match, boolean shouldNotifyMatchStart) {
      return new AutoValue_MatchResult_LoadMatchResult(LOAD_MATCH_SUCCESS, match, null,
          shouldNotifyMatchStart);
    }

    static LoadMatchResult failure(Throwable throwable) {
      return new AutoValue_MatchResult_LoadMatchResult(LOAD_MATCH_FAILURE, null, throwable, false);
    }

    static LoadMatchResult inFlight() {
      return new AutoValue_MatchResult_LoadMatchResult(LOAD_MATCH_IN_FLIGHT, null, null, false);
    }
  }

  @AutoValue abstract class LoadMatchDetailsResult implements MatchResult {
    abstract MatchDisplayable match();

    abstract boolean shouldNotifyMatchStart();

    static LoadMatchDetailsResult success(MatchDisplayable match, boolean shouldNotifyMatchStart) {
      return new AutoValue_MatchResult_LoadMatchDetailsResult(match, shouldNotifyMatchStart);
    }
  }

  @AutoValue abstract class NotifyMatchStartResult implements MatchResult {
    public abstract boolean shouldNotifyMatchStart();

    static NotifyMatchStartResult create(boolean shouldNotifyMatchStart) {
      return new AutoValue_MatchResult_NotifyMatchStartResult(shouldNotifyMatchStart);
    }
  }

  @AutoValue abstract class GetLastStateResult implements MatchResult {
    static GetLastStateResult create() {
      return new AutoValue_MatchResult_GetLastStateResult();
    }
  }

  enum Status {
    LOAD_MATCH_IN_FLIGHT, LOAD_MATCH_FAILURE, LOAD_MATCH_SUCCESS, //
  }
}
