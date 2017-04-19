package io.oldering.tvfoot.red.matches;

import com.google.auto.value.AutoValue;
import io.oldering.tvfoot.red.matches.displayable.MatchRowDisplayable;

interface MatchesAction {
  @AutoValue abstract class LoadFirstPageAction implements MatchesAction {
    public static LoadFirstPageAction create() {
      return new AutoValue_MatchesAction_LoadFirstPageAction();
    }
  }

  @AutoValue abstract class LoadNextPageAction implements MatchesAction {
    public abstract int currentPage();

    public static LoadNextPageAction create(int currentPage) {
      return new AutoValue_MatchesAction_LoadNextPageAction(currentPage);
    }
  }

  @AutoValue abstract class MatchRowClickAction implements MatchesAction {
    public abstract MatchRowDisplayable getMatch();

    public static MatchRowClickAction create(MatchRowDisplayable match) {
      return new AutoValue_MatchesAction_MatchRowClickAction(match);
    }
  }
}