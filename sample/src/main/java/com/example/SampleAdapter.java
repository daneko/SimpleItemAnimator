package com.example;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.annotation.Nonnull;

import butterknife.ButterKnife;
import butterknife.InjectView;

import fj.Unit;
import fj.data.Java;
import fj.data.List;
import fj.data.Option;

import rx.Observable;
import rx.subjects.PublishSubject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * TODO: {@link android.support.v7.widget.RecyclerView.ItemDecoration} ってなに
 * TODO: ViewType ってなに
 */
@Slf4j
public class SampleAdapter extends RecyclerView.Adapter<SampleAdapter.SampleViewHolder> {

    private java.util.List<SampleItem> items;

    private final PublishSubject<SampleViewHolder> itemClickSubject;
    private final PublishSubject<SampleViewHolder> itemLongClickSubject;

    @Getter
    private final Observable<SampleViewHolder> itemClickObservable;
    @Getter
    private final Observable<SampleViewHolder> itemLongClickObservable;

    public SampleAdapter(@Nonnull final java.util.List<SampleItem> initItems) {
        items = initItems;
        itemClickSubject = PublishSubject.create();
        itemLongClickSubject = PublishSubject.create();
        itemClickObservable = itemClickSubject.asObservable();
        itemLongClickObservable = itemLongClickSubject.asObservable();
    }

    public Unit add(@Nonnull final SampleItem item) {
        return insert(item, getItemCount());
    }

    public Unit insert(@Nonnull final SampleItem item, final int pos) {
        final int maxPos = getItemCount();
        final int insertPos = maxPos < pos ? maxPos : pos < 0 ? 0 : pos;
        items.add(insertPos, item);
        notifyItemInserted(insertPos);
        return Unit.unit();
    }

    public Unit replace(@Nonnull final List<SampleItem> itemList){
        items.clear();
        items.addAll(Java.<SampleItem>List_ArrayList().f(itemList));
        notifyDataSetChanged();
        return Unit.unit();
    }

    public Option<SampleItem> remove(final int pos) {
        final int maxPos = getItemCount() - 1;
        if (maxPos < 0) {
            return Option.none();
        }
        final int removePos = maxPos < pos ? maxPos : pos < 0 ? 0 : pos;
        final SampleItem item = items.remove(removePos);
        notifyItemRemoved(removePos);
        return Option.fromNull(item);
    }

    /**
     * change の時もAnimationが走るっぽいのでそれを試すよう
     *
     * @param newItem
     * @param pos
     * @return
     */
    public Option<SampleItem> change(final SampleItem newItem, final int pos) {
        final int maxPos = getItemCount() - 1;
        if (maxPos < 0) {
            return Option.none();
        }
        final int changePos = maxPos < pos ? maxPos : pos < 0 ? 0 : pos;
        final SampleItem prevItem = items.set(changePos, newItem);
        if (!prevItem.equals(newItem)) {
            notifyItemChanged(changePos);
        }
        return Option.fromNull(prevItem);
    }

    @Override
    public SampleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        final SampleViewHolder holder = new SampleViewHolder(view);
        view.setOnClickListener(v -> itemClickSubject.onNext(holder));
        view.setOnLongClickListener(v -> {
            itemLongClickSubject.onNext(holder);
            return true;
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(SampleViewHolder holder, int position) {
        holder.setItem(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class SampleViewHolder extends RecyclerView.ViewHolder {

        @InjectView(R.id.title)
        TextView title;
        @InjectView(R.id.description)
        TextView description;

        @Getter
        private SampleItem item;

        public SampleViewHolder(View view) {
            super(view);
            ButterKnife.inject(this, view);
        }

        Unit setItem(SampleItem item) {
            this.item = item;
            title.setText(item.getTitle());
            description.setText(item.getDescription());
            return Unit.unit();
        }

        public View getItemView() {
            return itemView;
        }
    }
}
