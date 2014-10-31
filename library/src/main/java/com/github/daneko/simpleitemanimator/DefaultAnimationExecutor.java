package com.github.daneko.simpleitemanimator;

import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;

import javax.annotation.Nonnull;

import fj.F;
import fj.F1Functions;
import fj.P1;
import fj.Unit;

import rx.Observable;

import lombok.extern.slf4j.Slf4j;

/**
 */
@Slf4j
class DefaultAnimationExecutor extends AbstAnimationExecutor implements AnimationTask.AnimationExecutor {
    private final SimpleItemAnimator parent;

    DefaultAnimationExecutor(@Nonnull final SimpleItemAnimator animator) {
        this.parent = animator;
    }

    /**
     * アニメーション実行命令を出して返す
     *
     * @return cancelを可能とするために {@link android.support.v4.view.ViewPropertyAnimatorCompat} を返す
     */
    @Override
    public Observable<ViewPropertyAnimatorCompat> execute(AnimationTask.AnimationEvent animationEvent) {
        return defaultExecute(animationEvent);
    }

    @Override
    F<Unit, P1<Unit>> startDispatcherSelector(@Nonnull final SimpleItemAnimator.Event event) {
        return F1Functions.lazy(u -> {
            switch (event.getParam().getEventType()) {
                case ADD:
                    parent.dispatchAddStarting(event.getTargetHolder());
                    break;
                case REMOVE:
                    parent.dispatchRemoveStarting(event.getTargetHolder());
                    break;
                case MOVE:
                    parent.dispatchMoveStarting(event.getTargetHolder());
                    break;
            }
            return u;
        });
    }

    @Override
    F<Unit, P1<Unit>> finishDispatcherSelector(@Nonnull final SimpleItemAnimator.Event event) {
        return F1Functions.lazy(u -> {
            switch (event.getParam().getEventType()) {
                case ADD:
                    parent.dispatchAddFinished(event.getTargetHolder());
                    break;
                case REMOVE:
                    parent.dispatchRemoveFinished(event.getTargetHolder());
                    break;
                case MOVE:
                    parent.dispatchMoveFinished(event.getTargetHolder());
                    break;
            }
            return u;
        });
    }

    @Override
    ViewPropertyAnimatorCompat createAnimator(@Nonnull final SimpleItemAnimator.Event event) {
        final ViewPropertyAnimatorCompat base = ViewCompat.animate(event.getTargetHolder().itemView);
        switch (event.getParam().getEventType()) {
            case ADD:
                return parent.getAddEventAnimation().f(base);
            case REMOVE:
                return parent.getRemoveEventAnimation().f(base);
            case MOVE:
                return parent.getMoveEventAnimation().f(base, event.getParam());
        }
        throw new IllegalArgumentException();
    }

    @Override
    long duration(@Nonnull final SimpleItemAnimator.EventType eventType) {

        switch (eventType) {
            case ADD:
                return parent.getAddDuration();
            case REMOVE:
                return parent.getRemoveDuration();
            case MOVE:
                return parent.getMoveDuration();
        }
        throw new IllegalArgumentException();
    }
}
