package com.github.daneko.simpleitemanimator;

import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.view.View;

import fj.F;
import fj.F2;
import fj.F3;
import fj.Unit;
import fj.data.Option;

/**
 * @see {@link android.support.v7.widget.DefaultItemAnimator}
 */
public class DefaultAnimations {
    public static F<View, Unit> addPrepareStateSetter() {
        return (view -> {
            ViewCompat.setAlpha(view, 0);
            return Unit.unit();
        });
    }

    public static F<View, Unit> removePrepareStateSetter() {
        return (view -> Unit.unit());
    }

    public static F2<View, SimpleItemAnimator.EventParam, Unit> movePrepareStateSetter() {

        return ((view, param) -> {
            if (param.getFromPoint().isNone() || param.getToPoint().isNone()) {
                return Unit.unit();
            }
            final int deltaX =
                    param.getToPoint().some().x -
                            param.getFromPoint().some().x +
                            (int) ViewCompat.getTranslationX(view);
            final int deltaY =
                    param.getToPoint().some().y -
                            param.getFromPoint().some().y +
                            (int) ViewCompat.getTranslationY(view);

            ViewCompat.setTranslationX(view, -deltaX);
            ViewCompat.setTranslationY(view, -deltaY);
            return Unit.unit();
        });
    }

    public static F3<View, Option<View>, SimpleItemAnimator.EventParam, Unit> changePrepareStateSetter() {
        return ((oldView, newViewOption, param) -> {
            if (param.getFromPoint().isNone() || param.getToPoint().isNone() || newViewOption.isNone()) {
                return Unit.unit();
            }
            final View newView = newViewOption.some();

            final int deltaX =
                    param.getToPoint().some().x -
                            param.getFromPoint().some().x +
                            (int) ViewCompat.getTranslationX(oldView);
            final int deltaY =
                    param.getToPoint().some().y -
                            param.getFromPoint().some().y +
                            (int) ViewCompat.getTranslationY(oldView);
            ViewCompat.setTranslationX(newView, -deltaX);
            ViewCompat.setTranslationY(newView, -deltaY);
            ViewCompat.setAlpha(newView, 0);
            return Unit.unit();
        });
    }

    public static F<ViewPropertyAnimatorCompat, ViewPropertyAnimatorCompat> addEventAnimation() {
        return (animator -> animator.alpha(1f));
    }

    public static F<ViewPropertyAnimatorCompat, ViewPropertyAnimatorCompat> removeEventAnimation() {
        return (animator -> animator.alpha(0f));
    }

    public static F2<ViewPropertyAnimatorCompat, SimpleItemAnimator.EventParam, ViewPropertyAnimatorCompat> moveEventAnimation() {
        return ((animator, param) -> animator.translationY(0f).translationX(0f));
    }

    public static F2<ViewPropertyAnimatorCompat, SimpleItemAnimator.EventParam, ViewPropertyAnimatorCompat> changeEventAnimationForOldView() {
        return ((animator, param) -> {
            if (param.getFromPoint().isNone() || param.getToPoint().isNone()) {
                return animator;
            }
            final int deltaX = param.getToPoint().some().x - param.getFromPoint().some().x;
            final int deltaY = param.getToPoint().some().y - param.getFromPoint().some().y;
            return animator.translationX(deltaX).translationY(deltaY).alpha(0f);
        });
    }

    public static F2<ViewPropertyAnimatorCompat, SimpleItemAnimator.EventParam, ViewPropertyAnimatorCompat> changeEventAnimationForNewView() {
        return ((animator, param) -> animator.translationX(0f).translationY(0f).alpha(0f));
    }
}
