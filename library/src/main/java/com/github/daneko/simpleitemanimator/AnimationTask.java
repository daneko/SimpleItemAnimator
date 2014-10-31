package com.github.daneko.simpleitemanimator;

import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.view.View;

import javax.annotation.Nonnull;

import fj.Unit;
import fj.data.Option;

import rx.Observable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
class AnimationTask {

    interface AnimationExecutor {
        Observable<ViewPropertyAnimatorCompat> execute(AnimationEvent event);
    }

    static AnimationExecutor of(
            @Nonnull final SimpleItemAnimator.EventType eType,
            @Nonnull final SimpleItemAnimator animator
            ){
        if(eType == SimpleItemAnimator.EventType.CHANGE){
            return new ChangeAnimationExecutor(animator);
        }
        return new DefaultAnimationExecutor(animator);
    }

    @Value
    static class AnimationEvent {
        SimpleItemAnimator.Event event;
        ViewPreState preState;
        /**
         * change event only
         * for newHolder
         */
        Option<ViewPreState> preStateOption;

        static AnimationEvent of(
                @Nonnull final SimpleItemAnimator.Event event,
                @Nonnull final ViewPreState preState
        ) {
            return new AnimationEvent(event, preState, Option.none());
        }

    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static class ViewPreState {
        private final float alpha;
        private final float pivotX;
        private final float pivotY;
        private final float rotationX;
        private final float rotationY;
        private final float scaleX;
        private final float scaleY;
        private final float translationX;
        private final float translationY;
        private final float translationZ;

        static ViewPreState of(View view) {
            return new ViewPreState(
                    ViewCompat.getAlpha(view),
                    ViewCompat.getPivotX(view),
                    ViewCompat.getPivotY(view),
                    ViewCompat.getRotationX(view),
                    ViewCompat.getRotationY(view),
                    ViewCompat.getScaleX(view),
                    ViewCompat.getScaleY(view),
                    ViewCompat.getTranslationX(view),
                    ViewCompat.getTranslationY(view),
                    ViewCompat.getTranslationZ(view)
            );
        }

        /**
         * @param view こちらの都合で内部的にViewを持たない仕様なので
         */
        Unit restoreState(@Nonnull final View view) {
            ViewCompat.setAlpha(view, alpha);
            ViewCompat.setPivotX(view, pivotX);
            ViewCompat.setPivotY(view, pivotY);
            ViewCompat.setRotationX(view, rotationX);
            ViewCompat.setRotationY(view, rotationY);
            ViewCompat.setScaleX(view, scaleX);
            ViewCompat.setScaleY(view, scaleY);
            ViewCompat.setTranslationX(view, translationX);
            ViewCompat.setTranslationY(view, translationY);
            ViewCompat.setTranslationZ(view, translationZ);
            return Unit.unit();
        }
    }
}
