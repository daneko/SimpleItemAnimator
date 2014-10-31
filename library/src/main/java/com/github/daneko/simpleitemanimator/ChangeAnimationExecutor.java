package com.github.daneko.simpleitemanimator;

import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.annotation.Nonnull;

import fj.F;
import fj.F1Functions;
import fj.P1;
import fj.Unit;
import fj.data.Option;

import rx.Observable;

import lombok.extern.slf4j.Slf4j;

/**
 */
@Slf4j
class ChangeAnimationExecutor extends AbstAnimationExecutor implements AnimationTask.AnimationExecutor {

    private final SimpleItemAnimator parent;

    ChangeAnimationExecutor(@Nonnull final SimpleItemAnimator animator) {
        this.parent = animator;
    }

    @Override
    public Observable<ViewPropertyAnimatorCompat> execute(AnimationTask.AnimationEvent animationEvent) {
        final Observable<ViewPropertyAnimatorCompat> oldViewAnimationObservable = defaultExecute(animationEvent);
        final Observable<ViewPropertyAnimatorCompat> newViewAnimationObservable = executeForNewHolder(animationEvent);

        if (animationEvent.getEvent().getSecondaryHolder().isNone() || animationEvent.getPreStateOption().isNone()) {
            return oldViewAnimationObservable;
        }

        return Observable.merge(oldViewAnimationObservable, newViewAnimationObservable);
    }

    Observable<ViewPropertyAnimatorCompat> executeForNewHolder(AnimationTask.AnimationEvent animationEvent) {
        final Option<RecyclerView.ViewHolder> newHolder = animationEvent.getEvent().getSecondaryHolder();
        if (newHolder.bind(h -> Option.fromNull(h.itemView)).isNone() || animationEvent.getPreStateOption().isNone()) {
            return Observable.empty();
        }

        final RecyclerView.ViewHolder holder = newHolder.some();
        final SimpleItemAnimator.Event event = animationEvent.getEvent();

        final F<Unit, P1<Unit>> startDispatcher = startDispatcherSelectorForNewHolder(holder);
        final F<Unit, P1<Unit>> finishDispatcher = finishDispatcherSelectorForNewHolder(holder);
        final F<View, Unit> defaultStateSetter = (animationEvent.getPreStateOption().some()::restoreState);
        final ViewPropertyAnimatorCompat animator =
                createAnimatorForNewHolder(holder.itemView, event.getParam()).setDuration(duration(event.getParam().getEventType()));

        return create(
                true,
                startDispatcher,
                finishDispatcher,
                defaultStateSetter,
                animator);
    }

    @Override
    F<Unit, P1<Unit>> startDispatcherSelector(@Nonnull final SimpleItemAnimator.Event event) {

        return F1Functions.lazy(u -> {
            parent.dispatchChangeStarting(event.getTargetHolder(), true);
            return u;
        });
    }

    @Override
    F<Unit, P1<Unit>> finishDispatcherSelector(@Nonnull final SimpleItemAnimator.Event event) {

        return F1Functions.lazy(u -> {
            parent.dispatchChangeFinished(event.getTargetHolder(), true);
            return u;
        });
    }

    @Override
    ViewPropertyAnimatorCompat createAnimator(@Nonnull final SimpleItemAnimator.Event event) {
        final ViewPropertyAnimatorCompat base = ViewCompat.animate(event.getTargetHolder().itemView);
        return parent.getChangeEventAnimationForOldView().f(base, event.getParam());
    }

    @Override
    long duration(@Nonnull final SimpleItemAnimator.EventType eventType) {
        return parent.getChangeDuration();
    }

    private F<Unit, P1<Unit>> startDispatcherSelectorForNewHolder(@Nonnull final RecyclerView.ViewHolder holder) {

        return F1Functions.<Unit, Unit>lazy(u -> {
            parent.dispatchChangeStarting(holder, false);
            return u;
        });
    }

    private F<Unit, P1<Unit>> finishDispatcherSelectorForNewHolder(@Nonnull final RecyclerView.ViewHolder holder) {

        return F1Functions.<Unit, Unit>lazy(u -> {
            parent.dispatchChangeFinished(holder, false);
            return u;
        });

    }

    private ViewPropertyAnimatorCompat createAnimatorForNewHolder(
            @Nonnull final View newView,
            @Nonnull final SimpleItemAnimator.EventParam param) {

        final ViewPropertyAnimatorCompat base = ViewCompat.animate(newView);
        // startタイミングはObservableでは管理できない…
        if(!parent.isChangeEventMix()) {
            base.setStartDelay(parent.getChangeDuration());
        }
        return parent.getChangeEventAnimationForNewView().f(base, param);
    }
}
