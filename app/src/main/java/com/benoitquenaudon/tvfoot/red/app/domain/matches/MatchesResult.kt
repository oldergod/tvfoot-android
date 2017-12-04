package com.benoitquenaudon.tvfoot.red.app.domain.matches

import com.benoitquenaudon.tvfoot.red.app.data.entity.FilterTeam
import com.benoitquenaudon.tvfoot.red.app.data.entity.Match
import com.benoitquenaudon.tvfoot.red.app.data.entity.Tag
import com.benoitquenaudon.tvfoot.red.app.data.entity.Team
import com.benoitquenaudon.tvfoot.red.app.domain.matches.filters.FiltersItemDisplayable.TeamSearchResultDisplayable
import com.benoitquenaudon.tvfoot.red.app.mvi.MviResult
import com.benoitquenaudon.tvfoot.red.util.MatchId
import com.benoitquenaudon.tvfoot.red.util.TeamCode
import com.benoitquenaudon.tvfoot.red.util.WillBeNotified

sealed class MatchesResult : MviResult {

  sealed class RefreshResult : MatchesResult() {
    data class Success(
        val matches: List<Match>,
        val willBeNotifiedPairs: Map<MatchId, WillBeNotified>
    ) : RefreshResult()

    data class Failure(val throwable: Throwable) : RefreshResult()
    object InFlight : RefreshResult()
  }

  sealed class LoadNextPageResult : MatchesResult() {

    data class Success(
        val pageIndex: Int,
        val matches: List<Match>,
        val willBeNotifiedPairs: Map<MatchId, WillBeNotified>
    ) : LoadNextPageResult()

    data class Failure(val throwable: Throwable) : LoadNextPageResult()

    object InFlight : LoadNextPageResult()
  }

  data class RefreshNotificationStatusResult(
      val matchId: MatchId,
      val willBeNotified: WillBeNotified
  ) : MatchesResult()

  sealed class FilterResult : MatchesResult() {

    object ClearFiltersResult : FilterResult()

    data class ToggleFilterCompetitionResult(val tagName: String) : FilterResult()

    data class ToggleFilterTeamResult(val teamCode: TeamCode) : FilterResult()

    sealed class LoadTagsResult : FilterResult() {
      data class Success(val tags: List<Tag>) : LoadTagsResult()
      data class Failure(val throwable: Throwable) : LoadTagsResult()
      object InFlight : LoadTagsResult()
    }

    sealed class SearchInputResult : FilterResult() {
      sealed class SearchTeamResult : SearchInputResult() {
        data class Success(val searchedInput: String, val teams: List<Team>) : SearchTeamResult()
        data class Failure(val throwable: Throwable) : SearchTeamResult()
        data class InFlight(val searchedInput: String) : SearchTeamResult()
      }

      object ClearSearchResult : SearchInputResult()
    }

    object ClearSearchInputResult : FilterResult()

    @Suppress("DataClassPrivateConstructor")
    data class SearchedTeamSelectedResult private constructor(
        val team: FilterTeam
    ) : FilterResult() {
      companion object {
        operator fun invoke(
            searchedTeam: TeamSearchResultDisplayable
        ): SearchedTeamSelectedResult {
          return SearchedTeamSelectedResult(
              FilterTeam(
                  code = searchedTeam.code,
                  type = searchedTeam.type,
                  name = searchedTeam.name,
                  country = searchedTeam.country
              )
          )
        }
      }
    }
  }
}
