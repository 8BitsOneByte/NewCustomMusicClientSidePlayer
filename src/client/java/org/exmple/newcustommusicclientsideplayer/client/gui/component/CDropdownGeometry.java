package org.exmple.newcustommusicclientsideplayer.client.gui.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Calculates dropdown bounds without rendering or depending on Minecraft GUI
 * classes.
 */
public final class CDropdownGeometry {
    private static final int SCROLLBAR_TRAVEL_MARGIN = 8;

    public enum ExpansionDirection {
        UP,
        DOWN
    }

    public static final Metrics DEFAULT_METRICS = new Metrics(
        4,
        0,
        2,
        18,
        1,
        6,
        32
    );

    private CDropdownGeometry() {
    }

    public static Layout calculate(Input input) {
        return calculate(input, DEFAULT_METRICS);
    }

    public static Layout calculate(Input input, Metrics metrics) {
        validate(input, metrics);

        int horizontalMargin = Math.min(metrics.screenMargin(), input.screenWidth() / 2);
        int verticalMargin = Math.min(metrics.screenMargin(), input.screenHeight() / 2);
        int availableScreenWidth = Math.max(0, input.screenWidth() - horizontalMargin * 2);
        int popupWidth = Math.min(input.controlWidth(), availableScreenWidth);
        int popupX = clamp(
            input.controlX(),
            horizontalMargin,
            Math.max(horizontalMargin, input.screenWidth() - horizontalMargin - popupWidth)
        );

        int anchorY;
        int availableHeight;
        if (input.direction() == ExpansionDirection.DOWN) {
            anchorY = input.controlY() + input.controlHeight() + metrics.popupGap();
            availableHeight = input.screenHeight() - verticalMargin - anchorY;
        } else {
            anchorY = input.controlY() - metrics.popupGap();
            availableHeight = anchorY - verticalMargin;
        }
        availableHeight = Math.max(0, availableHeight);

        int borderSpace = metrics.borderThickness() * 2;
        int rowsFittingAvailableSpace = Math.max(
            0,
            (availableHeight - borderSpace) / input.rowHeight()
        );
        int visibleItemCount = Math.min(
            input.optionCount(),
            Math.min(input.maxVisibleItems(), rowsFittingAvailableSpace)
        );

        if (popupWidth <= borderSpace + metrics.accessoryAreaWidth()
                || visibleItemCount == 0) {
            Rect emptyBounds = new Rect(popupX, anchorY, popupWidth, 0);
            return new Layout(
                input.direction(),
                emptyBounds,
                emptyBounds,
                emptyBounds,
                emptyBounds,
                List.of(),
                Optional.empty(),
                0,
                0
            );
        }

        int rowsHeight = visibleItemCount * input.rowHeight();
        int popupHeight = borderSpace + rowsHeight;
        int popupY = input.direction() == ExpansionDirection.DOWN
            ? anchorY
            : anchorY - popupHeight;
        Rect popupBounds = new Rect(popupX, popupY, popupWidth, popupHeight);

        int viewportX = popupX + metrics.borderThickness();
        int viewportY = popupY + metrics.borderThickness();
        int viewportWidth = popupWidth - borderSpace;
        Rect viewportBounds = new Rect(viewportX, viewportY, viewportWidth, rowsHeight);

        int maximumFirstVisibleIndex = Math.max(0, input.optionCount() - visibleItemCount);
        int firstVisibleIndex = clamp(
            input.firstVisibleIndex(),
            0,
            maximumFirstVisibleIndex
        );
        boolean scrollable = input.optionCount() > visibleItemCount;

        int separatorX = popupBounds.right() - metrics.accessoryAreaWidth();
        Rect dividerBounds = new Rect(
            separatorX,
            viewportY,
            metrics.dividerThickness(),
            rowsHeight
        );
        Rect accessoryBounds = new Rect(
            dividerBounds.right(),
            viewportY,
            Math.max(0, viewportBounds.right() - dividerBounds.right()),
            rowsHeight
        );
        int textWidth = Math.max(0, dividerBounds.x() - viewportX);

        Optional<Scrollbar> scrollbar = Optional.empty();
        if (scrollable) {
            if (accessoryBounds.width() >= metrics.scrollbarWidth()) {
                Rect trackBounds = new Rect(
                    accessoryBounds.x(),
                    viewportY,
                    accessoryBounds.width(),
                    rowsHeight
                );
                Rect thumbBounds = calculateThumbBounds(
                    trackBounds,
                    visibleItemCount,
                    input.optionCount(),
                    firstVisibleIndex,
                    maximumFirstVisibleIndex,
                    metrics.minimumScrollbarThumbHeight()
                );
                scrollbar = Optional.of(new Scrollbar(trackBounds, thumbBounds));
            }
        }

        List<VisibleRow> visibleRows = new ArrayList<>(visibleItemCount);
        for (int row = 0; row < visibleItemCount; row++) {
            int optionIndex = firstVisibleIndex + row;
            visibleRows.add(new VisibleRow(
                optionIndex,
                new Rect(
                    viewportX,
                    viewportY + row * input.rowHeight(),
                    viewportWidth,
                    input.rowHeight()
                ),
                new Rect(
                    viewportX,
                    viewportY + row * input.rowHeight(),
                    textWidth,
                    input.rowHeight()
                )
            ));
        }

        return new Layout(
            input.direction(),
            popupBounds,
            viewportBounds,
            dividerBounds,
            accessoryBounds,
            List.copyOf(visibleRows),
            scrollbar,
            visibleItemCount,
            firstVisibleIndex
        );
    }

    private static Rect calculateThumbBounds(
        Rect trackBounds,
        int visibleItemCount,
        int optionCount,
        int firstVisibleIndex,
        int maximumFirstVisibleIndex,
        int minimumThumbHeight
    ) {
        int proportionalHeight = (int) (
            trackBounds.height() * (visibleItemCount / (float) optionCount)
        );
        int maximumThumbHeight = Math.max(
            1,
            trackBounds.height() - SCROLLBAR_TRAVEL_MARGIN
        );
        int thumbHeight = Math.min(
            Math.max(minimumThumbHeight, proportionalHeight),
            maximumThumbHeight
        );
        int availableTravel = trackBounds.height() - thumbHeight;
        int thumbY = maximumFirstVisibleIndex == 0
            ? trackBounds.y()
            : trackBounds.y()
                + firstVisibleIndex * availableTravel / maximumFirstVisibleIndex;
        return new Rect(trackBounds.x(), thumbY, trackBounds.width(), thumbHeight);
    }

    private static void validate(Input input, Metrics metrics) {
        if (input == null) {
            throw new NullPointerException("input");
        }
        if (metrics == null) {
            throw new NullPointerException("metrics");
        }
        if (input.controlWidth() <= 0 || input.controlHeight() <= 0) {
            throw new IllegalArgumentException("control dimensions must be positive");
        }
        if (input.screenWidth() <= 0 || input.screenHeight() <= 0) {
            throw new IllegalArgumentException("screen dimensions must be positive");
        }
        if (input.rowHeight() <= 0) {
            throw new IllegalArgumentException("rowHeight must be positive");
        }
        if (input.maxVisibleItems() <= 0) {
            throw new IllegalArgumentException("maxVisibleItems must be positive");
        }
        if (input.optionCount() < 0) {
            throw new IllegalArgumentException("optionCount cannot be negative");
        }
        if (input.firstVisibleIndex() < 0) {
            throw new IllegalArgumentException("firstVisibleIndex cannot be negative");
        }
        if (input.direction() == null) {
            throw new NullPointerException("direction");
        }
        if (metrics.screenMargin() < 0
                || metrics.popupGap() < 0
                || metrics.borderThickness() < 0
                || metrics.accessoryAreaWidth() <= 0
                || metrics.dividerThickness() <= 0
                || metrics.scrollbarWidth() <= 0
                || metrics.minimumScrollbarThumbHeight() <= 0) {
            throw new IllegalArgumentException("metrics contain invalid dimensions");
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }

    public record Input(
        int controlX,
        int controlY,
        int controlWidth,
        int controlHeight,
        int screenWidth,
        int screenHeight,
        ExpansionDirection direction,
        int rowHeight,
        int maxVisibleItems,
        int optionCount,
        int firstVisibleIndex
    ) {
    }

    public record Metrics(
        int screenMargin,
        int popupGap,
        int borderThickness,
        int accessoryAreaWidth,
        int dividerThickness,
        int scrollbarWidth,
        int minimumScrollbarThumbHeight
    ) {
    }

    public record Layout(
        ExpansionDirection direction,
        Rect popupBounds,
        Rect viewportBounds,
        Rect dividerBounds,
        Rect accessoryBounds,
        List<VisibleRow> visibleRows,
        Optional<Scrollbar> scrollbar,
        int visibleItemCount,
        int firstVisibleIndex
    ) {
        public boolean canOpen() {
            return this.visibleItemCount > 0;
        }
    }

    public record VisibleRow(int optionIndex, Rect rowBounds, Rect textBounds) {
    }

    public record Scrollbar(Rect trackBounds, Rect thumbBounds) {
    }

    public record Rect(int x, int y, int width, int height) {
        public Rect {
            if (width < 0 || height < 0) {
                throw new IllegalArgumentException("rectangle dimensions cannot be negative");
            }
        }

        public int right() {
            return this.x + this.width;
        }

        public int bottom() {
            return this.y + this.height;
        }

        public boolean contains(double pointX, double pointY) {
            return pointX >= this.x
                && pointX < this.right()
                && pointY >= this.y
                && pointY < this.bottom();
        }
    }
}
