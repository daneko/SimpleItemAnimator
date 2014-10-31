package com.github.daneko.simpleitemanimator;

import android.graphics.Point;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.Nonnull;

import fj.F;
import fj.F2;
import fj.F3;
import fj.Unit;
import fj.data.Option;

import rx.Subscriber;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Getterが悲しい…
 * TODO: 例えばクリックでitem削除とかとした場合、連打するとおかしくなる…
 * <p>
 * memo: prepareは {@link #runPendingAnimations()} ではなく、イベント受信時に行なう
 */
@Slf4j
public class SimpleItemAnimator extends RecyclerView.ItemAnimator {

    @Getter(AccessLevel.PACKAGE)
    private final F<ViewPropertyAnimatorCompat, ViewPropertyAnimatorCompat> addEventAnimation;
    @Getter(AccessLevel.PACKAGE)
    private final F<ViewPropertyAnimatorCompat, ViewPropertyAnimatorCompat> removeEventAnimation;
    @Getter(AccessLevel.PACKAGE)
    private final F2<ViewPropertyAnimatorCompat, EventParam, ViewPropertyAnimatorCompat> moveEventAnimation;
    @Getter(AccessLevel.PACKAGE)
    private final F2<ViewPropertyAnimatorCompat, EventParam, ViewPropertyAnimatorCompat> changeEventAnimationForOldView;
    @Getter(AccessLevel.PACKAGE)
    private final F2<ViewPropertyAnimatorCompat, EventParam, ViewPropertyAnimatorCompat> changeEventAnimationForNewView;
    @Getter(AccessLevel.PACKAGE)
    private boolean changeEventMix;

    @Getter(AccessLevel.PACKAGE)
    private final F<View, Unit> prepareAddEventView;
    @Getter(AccessLevel.PACKAGE)
    private final F<View, Unit> prepareRemoveEventView;
    @Getter(AccessLevel.PACKAGE)
    private final F2<View, EventParam, Unit> prepareMoveEventView;
    @Getter(AccessLevel.PACKAGE)
    private final F3<View, Option<View>, EventParam, Unit> prepareChangeEventView;

    private SimpleItemAnimator(
            final F<ViewPropertyAnimatorCompat, ViewPropertyAnimatorCompat> addEventAnimation,
            final F<ViewPropertyAnimatorCompat, ViewPropertyAnimatorCompat> removeEventAnimation,
            final F2<ViewPropertyAnimatorCompat, EventParam, ViewPropertyAnimatorCompat> moveEventAnimation,
            final Option<F2<ViewPropertyAnimatorCompat, EventParam, ViewPropertyAnimatorCompat>> changeEventAnimationForOldView,
            final Option<F2<ViewPropertyAnimatorCompat, EventParam, ViewPropertyAnimatorCompat>> changeEventAnimationForNewView,
            final boolean isChangeEventMix,
            final F<View, Unit> prepareAddEventView,
            final F<View, Unit> prepareRemoveEventView,
            final F2<View, EventParam, Unit> prepareMoveEventView,
            final F3<View, Option<View>, EventParam, Unit> prepareChangeEventView) {

        this.addEventAnimation = addEventAnimation;
        this.removeEventAnimation = removeEventAnimation;
        this.moveEventAnimation = moveEventAnimation;
        this.changeEventAnimationForOldView = changeEventAnimationForOldView.toNull();
        this.changeEventAnimationForNewView = changeEventAnimationForNewView.toNull();
        final Boolean supportChange = changeEventAnimationForOldView.bind(cfo ->
                changeEventAnimationForNewView.map(cfn -> true)).orSome(false);
        setSupportsChangeAnimations(supportChange);
        this.changeEventMix = isChangeEventMix;
        this.prepareAddEventView = prepareAddEventView;
        this.prepareRemoveEventView = prepareRemoveEventView;
        this.prepareMoveEventView = prepareMoveEventView;
        this.prepareChangeEventView = prepareChangeEventView;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private F<ViewPropertyAnimatorCompat, ViewPropertyAnimatorCompat> addEventAnimation;
        private F<ViewPropertyAnimatorCompat, ViewPropertyAnimatorCompat> removeEventAnimation;
        private F2<ViewPropertyAnimatorCompat, EventParam, ViewPropertyAnimatorCompat> moveEventAnimation;
        private Option<F2<ViewPropertyAnimatorCompat, EventParam, ViewPropertyAnimatorCompat>> changeEventAnimationForOldView;
        private Option<F2<ViewPropertyAnimatorCompat, EventParam, ViewPropertyAnimatorCompat>> changeEventAnimationForNewView;
        private boolean isChangeEventMix;
        private F<View, Unit> prepareAddEventView;
        private F<View, Unit> prepareRemoveEventView;
        private F2<View, EventParam, Unit> prepareMoveEventView;
        private F3<View, Option<View>, EventParam, Unit> prepareChangeEventView;

        private long addDuration;
        private long removeDuration;
        private long moveDuration;
        private long changeDuration;

        Builder() {
            addEventAnimation = DefaultAnimations.addEventAnimation();
            removeEventAnimation = DefaultAnimations.removeEventAnimation();
            moveEventAnimation = DefaultAnimations.moveEventAnimation();

            changeEventAnimationForOldView = Option.none();
            changeEventAnimationForNewView = Option.none();
            isChangeEventMix = true;

            prepareAddEventView = DefaultAnimations.addPrepareStateSetter();
            prepareRemoveEventView = DefaultAnimations.removePrepareStateSetter();
            prepareMoveEventView = DefaultAnimations.movePrepareStateSetter();
            prepareChangeEventView = DefaultAnimations.changePrepareStateSetter();

            // same ItemAnimator default value
            addDuration = 120;
            removeDuration = 120;
            moveDuration = 250;
            changeDuration = 250;
        }

        /**
         * sample {@link DefaultAnimations#addPrepareStateSetter()}
         *
         * @param f (targetView -> {setStartState(targetView); return Unit.unit();})
         */
        public Builder preAddAnimationState(final F<View, Unit> f) {
            prepareAddEventView = f;
            return this;
        }

        /**
         * sample {@link DefaultAnimations#removeEventAnimation()}
         *
         * @param f (targetView -> {setStartState(targetView); return Unit.unit();})
         */
        public Builder preRemoveAnimationState(final F<View, Unit> f) {
            prepareRemoveEventView = f;
            return this;
        }

        /**
         * sample {@link DefaultAnimations#movePrepareStateSetter()}
         *
         * @param f ((targetView, param) -> {setStartState(targetView); return Unit.unit();})
         */
        public Builder preMoveAnimationState(final F2<View, EventParam, Unit> f) {
            prepareMoveEventView = f;
            return this;
        }

        /**
         * sample {@link DefaultAnimations#changePrepareStateSetter()}
         *
         * @param f ((oldView, newViewOption, param) -> {
         *          setStartStateForOldView(oldView);
         *          newViewOption.foreach(this::setStateForNewView);
         *          return Unit.unit();})
         */
        public Builder preChangeAnimationState(final F3<View, Option<View>, EventParam, Unit> f) {
            prepareChangeEventView = f;
            return this;
        }

        /**
         * sample {@link DefaultAnimations#addEventAnimation()}
         *
         * @param f ( animator -> animator.alpha(1f) )
         */
        public Builder addAnimation(@NonNull final F<ViewPropertyAnimatorCompat, ViewPropertyAnimatorCompat> f) {
            addEventAnimation = f;
            return this;
        }

        /**
         * sample {@link DefaultAnimations#removeEventAnimation()}
         *
         * @param f ( animator -> animator.alpha(1f) )
         */
        public Builder removeAnimation(@NonNull final F<ViewPropertyAnimatorCompat, ViewPropertyAnimatorCompat> f) {
            removeEventAnimation = f;
            return this;
        }

        /**
         * sample {@link DefaultAnimations#moveEventAnimation()}
         *
         * @param f ((animator, param) -> animator.translationY(0f).translationX(0f))
         */
        public Builder moveAnimation(@NonNull final F2<ViewPropertyAnimatorCompat, EventParam, ViewPropertyAnimatorCompat> f) {
            moveEventAnimation = f;
            return this;
        }

        /**
         * sample {@link DefaultAnimations#changeEventAnimationForOldView()}
         * and {@link DefaultAnimations#changeEventAnimationForNewView()}
         */
        public Builder changeAnimation(
                final F2<ViewPropertyAnimatorCompat, EventParam, ViewPropertyAnimatorCompat> forOld,
                final F2<ViewPropertyAnimatorCompat, EventParam, ViewPropertyAnimatorCompat> forNew) {
            changeEventAnimationForOldView = Option.fromNull(forOld);
            changeEventAnimationForNewView = Option.fromNull(forNew);
            return this;
        }

        public Builder addDuration(final long duration) {
            addDuration = duration;
            return this;
        }

        public Builder removeDuration(final long duration) {
            removeDuration = duration;
            return this;
        }

        public Builder moveDuration(final long duration) {
            moveDuration = duration;
            return this;
        }

        public Builder changeDuration(final long duration) {
            changeDuration = duration;
            return this;
        }

        /**
         * default true
         *
         * @param mix true : old view and new view animations run concurrency
         *            false : old view animation run before new view animation
         */
        public Builder isChangeAnimationMix(final boolean mix) {
            isChangeEventMix = mix;
            return this;
        }

        public SimpleItemAnimator build() {
            final SimpleItemAnimator animator = new SimpleItemAnimator(
                    addEventAnimation,
                    removeEventAnimation,
                    moveEventAnimation,
                    changeEventAnimationForOldView,
                    changeEventAnimationForNewView,
                    isChangeEventMix,
                    prepareAddEventView,
                    prepareRemoveEventView,
                    prepareMoveEventView,
                    prepareChangeEventView);

            animator.setAddDuration(addDuration);
            animator.setRemoveDuration(removeDuration);
            animator.setMoveDuration(moveDuration);
            animator.setChangeDuration(changeDuration);

            return animator;
        }
    }

    private java.util.List<AnimationTask.AnimationEvent> eventQueue =
            Collections.synchronizedList(new ArrayList<>());

    private java.util.Set<ViewPropertyAnimatorCompat> animationCanceler =
            Collections.synchronizedSet(new java.util.HashSet<>());

    /**
     * 順序重要
     */
    public enum EventType {
        REMOVE,
        MOVE,
        CHANGE,
        ADD
    }

    @Value
    public static class EventParam {
        EventType eventType;
        Option<Point> fromPoint;
        Option<Point> toPoint;

        static EventParam single(@Nonnull final EventType eventType) {
            return new EventParam(eventType, Option.none(), Option.none());
        }
    }

    @Value
    static class Event {
        RecyclerView.ViewHolder targetHolder;

        /**
         * change event only
         * for newHolder
         */
        Option<RecyclerView.ViewHolder> secondaryHolder;

        EventParam param;

        static Event of(
                @Nonnull final RecyclerView.ViewHolder target,
                @Nonnull final EventType eventType) {
            return new Event(target, Option.none(), EventParam.single(eventType));
        }
    }

    /**
     * Called when there are pending animations waiting to be started. This state
     * is governed by the return values from {@link #animateAdd(android.support.v7.widget.RecyclerView.ViewHolder) animateAdd()},
     * {@link #animateMove(android.support.v7.widget.RecyclerView.ViewHolder, int, int, int, int) animateMove()}, and
     * {@link #animateRemove(android.support.v7.widget.RecyclerView.ViewHolder) animateRemove()}, which inform the
     * RecyclerView that the ItemAnimator wants to be called later to start the
     * associated animations. runPendingAnimations() will be scheduled to be run
     * on the next frame.
     */
    public void runPendingAnimations() {
        log.trace("#runPendingAnimations");
        if (eventQueue.isEmpty()) {
            log.debug("#runPendingAnimations eventQueue empty??");
            return;
        }

        Collections.sort(eventQueue, (lhs, rhs) ->
                lhs.getEvent().getParam().getEventType().compareTo(rhs.getEvent().getParam().getEventType()));

        for(AnimationTask.AnimationEvent animationEvent : eventQueue) {

            AnimationTask.of(animationEvent.getEvent().getParam().getEventType(), this).
                    execute(animationEvent).
                    subscribe(new Subscriber<ViewPropertyAnimatorCompat>() {
                        // maybe max 2 (change animation)
                        private BlockingQueue<ViewPropertyAnimatorCompat> canceler =
                                new LinkedBlockingQueue<>(4);

                        @Override
                        public void onCompleted() {
                            removeCanceler();
                        }

                        @Override
                        public void onError(Throwable e) {
                            log.error("#runPendingAnimations onError", e);
                            removeCanceler();
                        }

                        @Override
                        @SneakyThrows(InterruptedException.class)
                        public void onNext(ViewPropertyAnimatorCompat animator) {
                            animationCanceler.add(animator);
                            canceler.put(animator);
                        }

                        @SneakyThrows(InterruptedException.class)
                        void removeCanceler() {
                            Option.fromNull(canceler.take()).foreach(animator -> {
                                animationCanceler.remove(animator);
                                return Unit.unit();
                            });

                            if (isEventEmpty()) {
                                dispatchAnimationsFinished();
                            }
                        }
                    });

        }
        eventQueue.clear();
        log.trace("#runPendingAnimations end");

    }

    void prepareState(@Nonnull final Event event) {
        log.trace("#prepareState");
        switch (event.getParam().getEventType()) {
            case ADD:
                prepareAddEventView.f(event.getTargetHolder().itemView);
                return;
            case REMOVE:
                prepareRemoveEventView.f(event.getTargetHolder().itemView);
                return;
            case MOVE:
                prepareMoveEventView.f(event.getTargetHolder().itemView, event.getParam());
                return;
            case CHANGE:
                prepareChangeEventView.f(
                        event.getTargetHolder().itemView,
                        event.getSecondaryHolder().bind(h -> Option.fromNull(h.itemView)),
                        event.getParam());
                return;
        }
        throw new IllegalArgumentException();
    }

    /**
     * Called when an item is removed from the RecyclerView. Implementors can choose
     * whether and how to animate that change, but must always call
     * {@link #dispatchRemoveFinished(android.support.v7.widget.RecyclerView.ViewHolder)} when done, either
     * immediately (if no animation will occur) or after the animation actually finishes.
     * The return value indicates whether an animation has been set up and whether the
     * ItemAnimator's {@link #runPendingAnimations()} method should be called at the
     * next opportunity. This mechanism allows ItemAnimator to set up individual animations
     * as separate calls to {@link #animateAdd(android.support.v7.widget.RecyclerView.ViewHolder) animateAdd()},
     * {@link #animateMove(android.support.v7.widget.RecyclerView.ViewHolder, int, int, int, int) animateMove()},
     * {@link #animateRemove(android.support.v7.widget.RecyclerView.ViewHolder) animateRemove()}, and
     * {@link #animateChange(android.support.v7.widget.RecyclerView.ViewHolder, android.support.v7.widget.RecyclerView.ViewHolder, int, int, int, int)} come in one by one,
     * then start the animations together in the later call to {@link #runPendingAnimations()}.
     * <p>
     * <p>This method may also be called for disappearing items which continue to exist in the
     * RecyclerView, but for which the system does not have enough information to animate
     * them out of view. In that case, the default animation for removing items is run
     * on those items as well.</p>
     *
     * @param holder The item that is being removed.
     * @return true if a later call to {@link #runPendingAnimations()} is requested,
     * false otherwise.
     */
    public boolean animateRemove(RecyclerView.ViewHolder holder) {
        log.trace("#animateRemove");
        if (holder == null || holder.itemView == null) {
            return false;
        }

        final Event event = Event.of(holder, EventType.REMOVE);
        final AnimationTask.AnimationEvent animationEvent = new AnimationTask.AnimationEvent(
                event,
                AnimationTask.ViewPreState.of(holder.itemView),
                Option.none());
        prepareState(animationEvent.getEvent());
        eventQueue.add(animationEvent);
        return true;
    }


    /**
     * Called when an item is added to the RecyclerView. Implementors can choose
     * whether and how to animate that change, but must always call
     * {@link #dispatchAddFinished(android.support.v7.widget.RecyclerView.ViewHolder)} when done, either
     * immediately (if no animation will occur) or after the animation actually finishes.
     * The return value indicates whether an animation has been set up and whether the
     * ItemAnimator's {@link #runPendingAnimations()} method should be called at the
     * next opportunity. This mechanism allows ItemAnimator to set up individual animations
     * as separate calls to {@link #animateAdd(android.support.v7.widget.RecyclerView.ViewHolder) animateAdd()},
     * {@link #animateMove(android.support.v7.widget.RecyclerView.ViewHolder, int, int, int, int) animateMove()},
     * {@link #animateRemove(android.support.v7.widget.RecyclerView.ViewHolder) animateRemove()}, and
     * {@link #animateChange(android.support.v7.widget.RecyclerView.ViewHolder, android.support.v7.widget.RecyclerView.ViewHolder, int, int, int, int)} come in one by one,
     * then start the animations together in the later call to {@link #runPendingAnimations()}.
     * <p>
     * <p>This method may also be called for appearing items which were already in the
     * RecyclerView, but for which the system does not have enough information to animate
     * them into view. In that case, the default animation for adding items is run
     * on those items as well.</p>
     *
     * @param holder The item that is being added.
     * @return true if a later call to {@link #runPendingAnimations()} is requested,
     * false otherwise.
     */
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        log.trace("#animateAdd");
        if (holder == null || holder.itemView == null) {
            return false;
        }
        final Event event = Event.of(holder, EventType.ADD);
        final AnimationTask.AnimationEvent animationEvent = new AnimationTask.AnimationEvent(
                event,
                AnimationTask.ViewPreState.of(holder.itemView),
                Option.none());
        prepareState(animationEvent.getEvent());
        eventQueue.add(animationEvent);
        return true;
    }

    /**
     * Called when an item is moved in the RecyclerView. Implementors can choose
     * whether and how to animate that change, but must always call
     * {@link #dispatchMoveFinished(android.support.v7.widget.RecyclerView.ViewHolder)} when done, either
     * immediately (if no animation will occur) or after the animation actually finishes.
     * The return value indicates whether an animation has been set up and whether the
     * ItemAnimator's {@link #runPendingAnimations()} method should be called at the
     * next opportunity. This mechanism allows ItemAnimator to set up individual animations
     * as separate calls to {@link #animateAdd(android.support.v7.widget.RecyclerView.ViewHolder) animateAdd()},
     * {@link #animateMove(android.support.v7.widget.RecyclerView.ViewHolder, int, int, int, int) animateMove()},
     * {@link #animateRemove(android.support.v7.widget.RecyclerView.ViewHolder) animateRemove()}, and
     * {@link #animateChange(android.support.v7.widget.RecyclerView.ViewHolder, android.support.v7.widget.RecyclerView.ViewHolder, int, int, int, int)} come in one by one,
     * then start the animations together in the later call to {@link #runPendingAnimations()}.
     *
     * @param holder The item that is being moved.
     * @return true if a later call to {@link #runPendingAnimations()} is requested,
     * false otherwise.
     */
    public boolean animateMove(RecyclerView.ViewHolder holder,
                               int fromX, int fromY,
                               int toX, int toY) {
        log.trace("#animateMove");
        if (holder == null || holder.itemView == null) {
            return false;
        }

        final int deltaX = toX - fromX - (int) ViewCompat.getTranslationX(holder.itemView);
        final int deltaY = toY - fromY - (int) ViewCompat.getTranslationY(holder.itemView);
        if (deltaX == 0 && deltaY == 0) {
            dispatchMoveFinished(holder);
            return false;
        }
        final EventParam param = new EventParam(
                EventType.MOVE,
                Option.fromNull(new Point(fromX, fromY)),
                Option.fromNull(new Point(toX, toY))
        );
        final Event event = new Event(
                holder,
                Option.none(),
                param);

        final AnimationTask.AnimationEvent animationEvent = new AnimationTask.AnimationEvent(
                event,
                AnimationTask.ViewPreState.of(holder.itemView),
                Option.none());
        prepareState(animationEvent.getEvent());
        eventQueue.add(animationEvent);
        return true;
    }

    /**
     * Called when an item is changed in the RecyclerView, as indicated by a call to
     * {@link android.support.v7.widget.RecyclerView.Adapter#notifyItemChanged(int)} or
     * {@link android.support.v7.widget.RecyclerView.Adapter#notifyItemRangeChanged(int, int)}.
     * <p>
     * Implementers can choose whether and how to animate changes, but must always call
     * {@link #dispatchChangeFinished(android.support.v7.widget.RecyclerView.ViewHolder, boolean)} for each non-null ViewHolder,
     * either immediately (if no animation will occur) or after the animation actually finishes.
     * The return value indicates whether an animation has been set up and whether the
     * ItemAnimator's {@link #runPendingAnimations()} method should be called at the
     * next opportunity. This mechanism allows ItemAnimator to set up individual animations
     * as separate calls to {@link #animateAdd(android.support.v7.widget.RecyclerView.ViewHolder) animateAdd()},
     * {@link #animateMove(android.support.v7.widget.RecyclerView.ViewHolder, int, int, int, int) animateMove()},
     * {@link #animateRemove(android.support.v7.widget.RecyclerView.ViewHolder) animateRemove()}, and
     * {@link #animateChange(android.support.v7.widget.RecyclerView.ViewHolder, android.support.v7.widget.RecyclerView.ViewHolder, int, int, int, int)} come in one by one,
     * then start the animations together in the later call to {@link #runPendingAnimations()}.
     *
     * @param oldHolder The original item that changed.
     * @param newHolder The new item that was created with the changed content. Might be null
     * @param fromLeft  Left of the old view holder
     * @param fromTop   Top of the old view holder
     * @param toLeft    Left of the new view holder
     * @param toTop     Top of the new view holder
     * @return true if a later call to {@link #runPendingAnimations()} is requested,
     * false otherwise.
     */
    public boolean animateChange(RecyclerView.ViewHolder oldHolder,
                                 RecyclerView.ViewHolder newHolder,
                                 int fromLeft, int fromTop, int toLeft, int toTop) {
        log.trace("#animateChange");
        if (!getSupportsChangeAnimations() || oldHolder == null || oldHolder.itemView == null) {
            return false;
        }
        final Option<RecyclerView.ViewHolder> newHolderOption = Option.fromNull(newHolder);
        final EventParam param = new EventParam(
                EventType.CHANGE,
                Option.fromNull(new Point(fromLeft, fromTop)),
                Option.fromNull(new Point(toLeft, toTop)));
        final Event event = new Event(
                oldHolder,
                newHolderOption,
                param);

        final AnimationTask.AnimationEvent animationEvent = new AnimationTask.AnimationEvent(
                event,
                AnimationTask.ViewPreState.of(oldHolder.itemView),
                newHolderOption.map(h -> AnimationTask.ViewPreState.of(h.itemView)));
        prepareState(animationEvent.getEvent());
        eventQueue.add(animationEvent);
        return true;
    }

    /**
     * Method called when an animation on a view should be ended immediately.
     * This could happen when other events, like scrolling, occur, so that
     * animating views can be quickly put into their proper end locations.
     * Implementations should ensure that any animations running on the item
     * are canceled and affected properties are set to their end values.
     * Also, appropriate dispatch methods (e.g., {@link #dispatchAddFinished(android.support.v7.widget.RecyclerView.ViewHolder)}
     * should be called since the animations are effectively done when this
     * method is called.
     *
     * @param item The item for which an animation should be stopped.
     */
    public void endAnimation(RecyclerView.ViewHolder item) {
        log.trace("#endAnimation");
    }

    /**
     * Method called when all item animations should be ended immediately.
     * This could happen when other events, like scrolling, occur, so that
     * animating views can be quickly put into their proper end locations.
     * Implementations should ensure that any animations running on any items
     * are canceled and affected properties are set to their end values.
     * Also, appropriate dispatch methods (e.g., {@link #dispatchAddFinished(android.support.v7.widget.RecyclerView.ViewHolder)}
     * should be called since the animations are effectively done when this
     * method is called.
     */
    public void endAnimations() {
        log.trace("#endAnimations");
        for (ViewPropertyAnimatorCompat animator : animationCanceler) {
            log.trace("#endAnimations call cancel");
            animator.cancel();
        }
        animationCanceler.clear();
        eventQueue.clear();
    }

    /**
     * Method which returns whether there are any item animations currently running.
     * This method can be used to determine whether to delay other actions until
     * animations end.
     *
     * @return true if there are any item animations currently running, false otherwise.
     */
    public boolean isRunning() {
        return !isEventEmpty();
    }

    private boolean isEventEmpty() {
        return eventQueue.isEmpty() && animationCanceler.isEmpty();
    }
}
