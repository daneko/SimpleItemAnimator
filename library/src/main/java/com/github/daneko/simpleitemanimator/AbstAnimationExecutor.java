package com.github.daneko.simpleitemanimator;

import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.view.View;

import javax.annotation.Nonnull;

import fj.F;
import fj.P1;
import fj.Unit;

import rx.Observable;

import lombok.extern.slf4j.Slf4j;

/**
 */
@Slf4j
abstract class AbstAnimationExecutor {

    /**
     * アニメーション実行命令を出して返す
     *
     * @return cancelを可能とするために {@link android.support.v4.view.ViewPropertyAnimatorCompat} を返す
     */
    protected Observable<ViewPropertyAnimatorCompat> defaultExecute(
            @Nonnull final AnimationTask.AnimationEvent animationEvent) {

        final SimpleItemAnimator.Event event = animationEvent.getEvent();
        final F<Unit, P1<Unit>> startDispatcher = startDispatcherSelector(event);
        final F<Unit, P1<Unit>> finishDispatcher = finishDispatcherSelector(event);
        final F<View, Unit> defaultStateSetter = (animationEvent.getPreState()::restoreState);
        final SimpleItemAnimator.EventType eventType = event.getParam().getEventType();
        final ViewPropertyAnimatorCompat animator =
                createAnimator(event).setDuration(duration(eventType));

        return create(
                eventType == SimpleItemAnimator.EventType.ADD || eventType == SimpleItemAnimator.EventType.MOVE,
                startDispatcher,
                finishDispatcher,
                defaultStateSetter,
                animator);
    }

    protected Observable<ViewPropertyAnimatorCompat> create(
            final boolean isRestoreView,
            @Nonnull final F<Unit, P1<Unit>> startDispatcher,
            @Nonnull final F<Unit, P1<Unit>> finishDispatcher,
            @Nonnull final F<View, Unit> defaultStateSetter,
            @Nonnull final ViewPropertyAnimatorCompat animator) {

        return Observable.create((Observable.OnSubscribe<ViewPropertyAnimatorCompat>) s -> {

            // fuck'in code...
            // for existing listener…
            // https://developer.android.com/reference/android/support/v4/view/ViewPropertyAnimatorCompat.html#setListener(android.support.v4.view.ViewPropertyAnimatorListener)
            animator.setListener(new ViewPropertyAnimatorListener() {
                @Override
                public void onAnimationStart(View view) {
                    log.trace("start animation");
                    startDispatcher.f(Unit.unit());
                }

                @Override
                public void onAnimationEnd(View view) {
                    log.trace("end animation");
                    finish(view);
                    finishDispatcher.f(Unit.unit());
                    s.onCompleted();
                }

                @Override
                public void onAnimationCancel(View view) {
                    log.trace("cancel animation");
                    finish(view);
                    s.onCompleted();
                }

                void finish(View view) {
                    animator.setListener(null);
                    animator.setStartDelay(0);
                    /*
                     * TODO: hope that perhaps there is another way
                     * if view is not Gone, (especially remove view, ) this view can occur event...
                     */
                    if (isRestoreView) {
                        defaultStateSetter.f(view);
                    } else {
                        view.setVisibility(View.GONE);
                    }
                }
            });
            s.onNext(animator);
        });
    }

    abstract F<Unit, P1<Unit>> startDispatcherSelector(@Nonnull final SimpleItemAnimator.Event event);

    abstract F<Unit, P1<Unit>> finishDispatcherSelector(@Nonnull final SimpleItemAnimator.Event event);

    abstract ViewPropertyAnimatorCompat createAnimator(@Nonnull final SimpleItemAnimator.Event event);

    abstract long duration(@Nonnull final SimpleItemAnimator.EventType eventType);
}
