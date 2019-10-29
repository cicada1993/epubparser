import React, { Component } from 'react'
import Board from './Board'
import './game.css'
export default class Game extends Component {
    render() {
        return (
            <div className='game'>
                <div>this is game page</div>
                <div className='game-board'>
                    <Board />
                </div>
                <div className = 'game-info'>

                </div>
            </div>
        )
    }
}