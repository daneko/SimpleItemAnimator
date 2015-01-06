package com.example;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.github.daneko.simpleitemanimator.SimpleItemAnimator;

import java.util.SortedSet;
import java.util.TreeSet;

import butterknife.ButterKnife;
import butterknife.InjectView;

import fj.F2;
import fj.P;
import fj.Unit;
import fj.data.Java;
import fj.data.List;
import fj.data.Stream;

import rx.android.app.AppObservable;
import rx.schedulers.Schedulers;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class MyActivity extends Activity {

    @InjectView(R.id.recycler_view)
    RecyclerView recyclerView;
    private SampleAdapter adapter;

    private int calcItemToBottomDuration(final View view) {
        final int decoratedTop = recyclerView.getLayoutManager().getDecoratedTop(view);
        return recyclerView.getHeight() - decoratedTop;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_activity);
        ButterKnife.inject(this);

        // listが固定サイズなら使うとPerformances upらしい
//       recyclerView.setHasFixedSize(true);

        // Linear 以外に Grid Animation StaggeredGrid とかあるっぽいけど差異は未確認
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SampleAdapter(Java.<SampleItem>List_ArrayList().f(defaultItems()));
        recyclerView.setAdapter(adapter);

        recyclerView.setItemAnimator(
                SimpleItemAnimator.builder().
                        preAddAnimationState(v -> {
                            ViewCompat.setAlpha(v, 1);
                            ViewCompat.setScaleX(v, 0);
                            ViewCompat.setScaleY(v, 0);
                            return Unit.unit();
                        }).
                        addAnimation(animator -> animator.scaleX(1).scaleY(1)).
                        addDuration(500).
                        removeAnimation(animator ->
                                animator.translationXBy(recyclerView.getWidth())).
                        preChangeAnimationState((oldV, newVOpt, param) -> {
                            ViewCompat.setAlpha(oldV, 1);
                            newVOpt.foreach(newV -> {
                                ViewCompat.setAlpha(newV, 0);
                                ViewCompat.setRotationX(newV, -180);
                                return Unit.unit();
                            });
                            return Unit.unit();
                        }).
                        changeAnimation(
                                (forOld, param) -> forOld.rotationX(180).alpha(0),
                                (forNew, param) -> forNew.rotationX(0).alpha(1)).
                        isChangeAnimationMix(false).
                        changeDuration(500).
                        build()
        );

        /**
         * アイテムのClickはAdapter内でどうにか頑張ることになるようだ
         * {@link android.widget.ListView#setOnItemClickListener(android.widget.AdapterView.OnItemClickListener)} 的なものは無い
         */
        AppObservable.bindActivity(this, adapter.getItemClickObservable()).
                subscribeOn(Schedulers.io()).
                map(holder -> P.p(holder.getItem(), recyclerView.getChildPosition(holder.getItemView()))).
                subscribe(clickItem -> adapter.remove(clickItem._2()));

        AppObservable.bindActivity(this, adapter.getItemLongClickObservable()).
                subscribeOn(Schedulers.io()).
                map(holder -> P.p(holder.getItem(), recyclerView.getChildPosition(holder.getItemView()))).
                subscribe(clickItem -> adapter.change(clickItem._1().withDescription("change!"), clickItem._2()));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    private SortedSet<SimpleItemAnimator.EventType> set = new TreeSet<>();

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()){
            case R.id.action_add:
                adapter.insert(new SampleItem("add!", "add!"), 5);
                return true;
            case R.id.action_reset:
                adapter.replace(defaultItems());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private List<SampleItem> defaultItems() {
        final F2<SampleItem, Integer, SampleItem> withIndex = ((item, i) ->
                new SampleItem(item.getTitle() + i.toString(), item.getDescription() + i.toString())
        );
        return Stream.
                repeat(new SampleItem("title", "description")).
                zipIndex().
                map(item -> withIndex.f(item._1(), item._2())).
                take(10).toList();
    }
}
