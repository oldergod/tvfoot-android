package com.benoitquenaudon.tvfoot.red.app.domain.matches;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.benoitquenaudon.tvfoot.red.R;
import com.benoitquenaudon.tvfoot.red.app.domain.matches.displayable.HeaderRowDisplayable;
import com.benoitquenaudon.tvfoot.red.app.domain.matches.displayable.LoadingRowDisplayable;
import com.benoitquenaudon.tvfoot.red.app.domain.matches.displayable.MatchRowDisplayable;
import com.benoitquenaudon.tvfoot.red.app.domain.matches.displayable.MatchesItemDisplayable;
import com.benoitquenaudon.tvfoot.red.app.domain.matches.displayable.MatchesItemDisplayableDiffUtilCallback;
import com.benoitquenaudon.tvfoot.red.app.injection.scope.ActivityScope;
import com.benoitquenaudon.tvfoot.red.databinding.MatchesRowHeaderBinding;
import com.benoitquenaudon.tvfoot.red.databinding.MatchesRowMatchBinding;
import com.benoitquenaudon.tvfoot.red.databinding.RowLoadingBinding;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

@ActivityScope public class MatchesAdapter extends RecyclerView.Adapter<MatchesItemViewHolder> {
  private List<MatchesItemDisplayable> matchesItems = Collections.emptyList();
  private PublishSubject<MatchRowDisplayable> matchRowClickObservable = PublishSubject.create();

  @Inject MatchesAdapter() {
  }

  @Override public MatchesItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
    ViewDataBinding binding = DataBindingUtil.inflate(layoutInflater, viewType, parent, false);

    switch (viewType) {
      case R.layout.matches_row_header:
        return new MatchesItemViewHolder.MatchHeaderViewHolder((MatchesRowHeaderBinding) binding);
      case R.layout.matches_row_match:
        return new MatchesItemViewHolder.MatchRowViewHolder((MatchesRowMatchBinding) binding,
            this);
      case R.layout.row_loading:
        return new MatchesItemViewHolder.LoadingRowViewHolder((RowLoadingBinding) binding);
      default:
        throw new UnsupportedOperationException(
            "don't know how to deal with this viewType: " + viewType);
    }
  }

  Observable<MatchRowDisplayable> getMatchRowClickObservable() {
    return matchRowClickObservable;
  }

  @SuppressWarnings("unchecked") @Override
  public void onBindViewHolder(MatchesItemViewHolder holder, int position) {
    holder.bind(matchesItems.get(position));
  }

  @Override public void onViewRecycled(MatchesItemViewHolder holder) {
    super.onViewRecycled(holder);
    holder.unbind();
  }

  @Override public int getItemCount() {
    return matchesItems.size();
  }

  @Override public int getItemViewType(int position) {
    MatchesItemDisplayable item = matchesItems.get(position);
    if (item instanceof MatchRowDisplayable) {
      return R.layout.matches_row_match;
    }
    if (item instanceof HeaderRowDisplayable) {
      return R.layout.matches_row_header;
    }
    if (item instanceof LoadingRowDisplayable) {
      return R.layout.row_loading;
    }
    throw new UnsupportedOperationException("Don't know how to deal with this item: " + item);
  }

  public void onClick(MatchRowDisplayable match) {
    matchRowClickObservable.onNext(match);
  }

  private MatchesItemDisplayableDiffUtilCallback diffUtilCallback =
      new MatchesItemDisplayableDiffUtilCallback();

  void setMatchesItems(List<MatchesItemDisplayable> newItems) {
    List<MatchesItemDisplayable> oldItems = this.matchesItems;
    this.matchesItems = newItems;

    diffUtilCallback.bindItems(oldItems, newItems);
    // TODO(benoit) calculate diff on worker thread
    // https://github.com/googlesamples/android-architecture-components/blob/master/GithubBrowserSample/app/src/main/java/com/android/example/github/ui/common/DataBoundListAdapter.java#L77
    DiffUtil.calculateDiff(diffUtilCallback, true).dispatchUpdatesTo(this);
  }
}
