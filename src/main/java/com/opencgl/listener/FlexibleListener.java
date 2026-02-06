package com.opencgl.listener;


import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * @author Chance.W
 * @version 1.0
 * @CreateDate 2023/06/03 16:56
 * @since v2.0
 */
public class FlexibleListener implements EventHandler<MouseEvent> {


    /**
     * 判定是否为调整窗口状态的范围与边界距离
     */
    private static final double RESIZE_WIDTH = 5.0D;
    /**
     * 窗口最小宽度
     */
    private static final double MIN_WIDTH = 1280.0D;
    /**
     * 窗口最小高度
     */
    private static final double MIN_HEIGHT = 720.0D;

    /**
     * 是否处于调整窗口状态（实例变量，避免多窗口冲突）
     */
    private boolean within;

    private final Stage stage;

    private Node node = null;

    public FlexibleListener(Stage stage) {
        this.stage = stage;
    }

    @Override
    public void handle(MouseEvent event) {
        if (event.getEventType() == MouseEvent.MOUSE_MOVED) {
            //事件被消费，不在执行其他任务
            event.consume();
            double x = event.getSceneX();
            double y = event.getSceneY();
            double width = stage.getWidth();
            double height = stage.getHeight();

            // 鼠标光标初始为默认类型，若未进入调整窗口状态，保持默认类型
            PositionResult result = stage.isMaximized() || stage.isFullScreen()
                ? new PositionResult(Cursor.DEFAULT, false)
                : position(x, y, width, height);
            this.within = result.within;
            node.setCursor(result.cursor);
        }

        if (event.getEventType() == MouseEvent.MOUSE_DRAGGED && within) {
            double x = event.getSceneX();
            double y = event.getSceneY();
            // 保存窗口改变后的x、y坐标和宽度、高度，用于预判是否会小于最小宽度、最小高度
            double nextX = stage.getX();
            double nextY = stage.getY();
            double nextWidth = stage.getWidth();
            double nextHeight = stage.getHeight();

            //鼠标焦点位于左侧边界，执行左右移动
            if (Cursor.NW_RESIZE.equals(node.getCursor())
                || Cursor.W_RESIZE.equals(node.getCursor())
                || Cursor.SW_RESIZE.equals(node.getCursor())) {
                double width = Math.max(nextWidth - x, MIN_WIDTH);
                nextX = nextX + nextWidth - width;
                nextWidth = width;
            }

            //鼠标焦点位于右侧边界，执行左右移动
            if (Cursor.NE_RESIZE.equals(node.getCursor())
                || Cursor.E_RESIZE.equals(node.getCursor())
                || Cursor.SE_RESIZE.equals(node.getCursor())) {
                nextWidth = Math.max(x, MIN_WIDTH);
            }

            //鼠标焦点位于顶部边界，执行上下移动
            if (Cursor.SW_RESIZE.equals(node.getCursor())
                || Cursor.SE_RESIZE.equals(node.getCursor())
                || Cursor.S_RESIZE.equals(node.getCursor())) {
                nextHeight = Math.max(y, MIN_HEIGHT);
            }

            //鼠标焦点位于底部边界，执行上下移动
            if (Cursor.NW_RESIZE.equals(node.getCursor())
                || Cursor.N_RESIZE.equals(node.getCursor())
                || Cursor.NE_RESIZE.equals(node.getCursor())) {
                double height = Math.max(nextHeight - y, MIN_HEIGHT);
                nextY = nextY + nextHeight - height;
                nextHeight = height;
            }

            // 最后统一改变窗口的x、y坐标和宽度、高度，可以防止刷新频繁出现的屏闪情况
            stage.setX(nextX);
            stage.setY(nextY);
            stage.setWidth(nextWidth);
            stage.setHeight(nextHeight);
        }
    }

    public void enableDrag(Node node) {
        this.node = node;
        node.setOnMouseMoved(this);
        node.setOnMouseDragged(this);
        node.addEventHandler(MouseEvent.MOUSE_EXITED, event -> resetCursor());
        node.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> resetCursor());
        stage.maximizedProperty().addListener((observable, oldValue, maximized) -> {
            if (maximized) resetCursor();
        });
        stage.fullScreenProperty().addListener((observable, oldValue, fullScreen) -> {
            if (fullScreen) resetCursor();
        });
    }

    private void resetCursor() {
        within = false;
        if (node != null) node.setCursor(Cursor.DEFAULT);
    }

    static Cursor cursorFor(double x, double y, double width, double height) {
        return position(x, y, width, height).cursor;
    }

    /**
     * 判断当前鼠标所在的位置并根据位置显示对应的形状
     * 以及是否触发对应的伸缩事件
     *
     * @param x      鼠标坐标x
     * @param y      鼠标坐标y
     * @param width  stage窗口的宽度
     * @param height stage窗口的高度
     * @return 位置结果，包含光标类型和是否在调整区域
     */
    private static PositionResult position(double x, double y, double width, double height) {
        //左上判断
        if (x < RESIZE_WIDTH && x >= 0 && y >= 0 && y < RESIZE_WIDTH) {
            return new PositionResult(Cursor.NW_RESIZE, true);
        }

        //左侧判断
        if (x < RESIZE_WIDTH && x >= 0 && y >= RESIZE_WIDTH && y < height - RESIZE_WIDTH) {
            return new PositionResult(Cursor.W_RESIZE, true);
        }

        //左下判断
        if (x < RESIZE_WIDTH && x >= 0 && y >= height - RESIZE_WIDTH && y < height) {
            return new PositionResult(Cursor.SW_RESIZE, true);
        }

        //上侧判断
        if (x < width - RESIZE_WIDTH && x >= RESIZE_WIDTH && y >= 0 && y < RESIZE_WIDTH) {
            return new PositionResult(Cursor.N_RESIZE, true);
        }

        //上右判断
        if (x < width && x >= width - RESIZE_WIDTH && y >= 0 && y < RESIZE_WIDTH) {
            return new PositionResult(Cursor.NE_RESIZE, true);
        }

        //右侧判断
        if (x < width && x >= width - RESIZE_WIDTH && y >= RESIZE_WIDTH && y < height - RESIZE_WIDTH) {
            return new PositionResult(Cursor.E_RESIZE, true);
        }

        //右下判断
        if (x < width && x >= width - RESIZE_WIDTH && y >= height - RESIZE_WIDTH && y < height) {
            return new PositionResult(Cursor.SE_RESIZE, true);
        }

        //下方判断
        if (x < width - RESIZE_WIDTH && x >= RESIZE_WIDTH && y >= height - RESIZE_WIDTH && y < height) {
            return new PositionResult(Cursor.S_RESIZE, true);
        }

        return new PositionResult(Cursor.DEFAULT, false);
    }

    /**
     * 位置结果内部类
     */
    private static class PositionResult {
        final Cursor cursor;
        final boolean within;

        PositionResult(Cursor cursor, boolean within) {
            this.cursor = cursor;
            this.within = within;
        }
    }
}
