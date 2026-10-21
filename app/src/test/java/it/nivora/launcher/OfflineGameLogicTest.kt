package it.nivora.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineGameLogicTest {
    @Test
    fun snakeTurnBufferRejectsImmediateReverse() {
        assertEquals(SnakeDirection.RIGHT, requestSnakeTurn(SnakeDirection.RIGHT, SnakeDirection.LEFT))
        assertEquals(SnakeDirection.LEFT, requestSnakeTurn(SnakeDirection.LEFT, SnakeDirection.RIGHT))
        assertEquals(SnakeDirection.UP, requestSnakeTurn(SnakeDirection.UP, SnakeDirection.DOWN))
        assertEquals(SnakeDirection.DOWN, requestSnakeTurn(SnakeDirection.DOWN, SnakeDirection.UP))
    }

    @Test
    fun snakeTurnBufferAllowsPerpendicularTurn() {
        assertEquals(SnakeDirection.UP, requestSnakeTurn(SnakeDirection.RIGHT, SnakeDirection.UP))
        assertEquals(SnakeDirection.DOWN, requestSnakeTurn(SnakeDirection.LEFT, SnakeDirection.DOWN))
    }

    @Test
    fun snakeMovesOneCellWithoutChangingLength() {
        val initial = SnakeState(body = listOf(7 to 10, 6 to 10, 5 to 10), food = 0 to 0)
        val next = advanceSnake(initial, SnakeDirection.RIGHT)

        assertEquals(8 to 10, next.body.first())
        assertEquals(3, next.body.size)
        assertEquals(0, next.score)
        assertFalse(next.over)
    }

    @Test
    fun snakeEatingFoodGrowsAndAwardsPoints() {
        val initial = SnakeState(body = listOf(7 to 10, 6 to 10, 5 to 10), food = 8 to 10)
        val next = advanceSnake(initial, SnakeDirection.RIGHT)

        assertEquals(4, next.body.size)
        assertEquals(10, next.score)
        assertTrue(next.food !in next.body)
        assertFalse(next.over)
    }

    @Test
    fun snakeCollisionWithWallEndsGame() {
        val initial = SnakeState(body = listOf(15 to 10, 14 to 10, 13 to 10), food = 0 to 0)
        val next = advanceSnake(initial, SnakeDirection.RIGHT)

        assertTrue(next.over)
        assertEquals(initial.body, next.body)
    }

    @Test
    fun snakeCannotMoveIntoItsOwnBody() {
        val initial = SnakeState(body = listOf(2 to 2, 2 to 3, 1 to 3, 1 to 2), food = 10 to 10)
        val next = advanceSnake(initial, SnakeDirection.DOWN)

        assertTrue(next.over)
    }

    @Test
    fun blockCanMoveSidewaysWhenSpaceIsFree() {
        val initial = BlockState(pieceType = 2, pieceX = 3, pieceY = 3)
        val next = moveBlock(initial, dx = -1)

        assertEquals(2, next.pieceX)
        assertEquals(initial.pieceY, next.pieceY)
        assertFalse(next.over)
        assertEquals(initial.board, next.board)
    }

    @Test
    fun blockCannotMoveOutsideLeftEdge() {
        // The line piece has no empty leading column, so moving left from x=0 must be rejected.
        val initial = BlockState(pieceType = 1, pieceX = 0, pieceY = 3)
        val next = moveBlock(initial, dx = -1)

        assertEquals(0, next.pieceX)
        assertEquals(initial.board, next.board)
    }

    @Test
    fun hardDropLocksThePieceIntoTheBoard() {
        val initial = BlockState(pieceType = 2, pieceX = 3, pieceY = 0)
        val next = moveBlock(initial, hardDrop = true)

        assertEquals(4, next.board.sumOf { row -> row.count { it != 0 } })
        assertFalse(next.over)
    }

    @Test
    fun completedRowIsClearedAndScoresOneHundredPoints() {
        val board = MutableList(18) { MutableList(10) { 0 } }
        for (x in 0 until 10) {
            if (x !in 3..6) board[17][x] = 2
        }
        val initial = BlockState(board = board.map { it.toList() }, pieceType = 1, pieceX = 3, pieceY = 0)
        val next = moveBlock(initial, hardDrop = true)

        assertEquals(100, next.score)
        assertTrue(next.board.all { row -> row.all { it == 0 } })
    }
}
