import React, { Component } from 'react'
import { HashRouter, Switch, Route, Link } from 'react-router-dom'
import App from './App'
import Game from './game/Game'
import Mobx from './mobx/Mobx'
import Flyio from './flyio/Flyio'
import Platform from './platform/Platform'
import LaunchApp from './launchapp/LaunchApp'
import ChooseFile from './choosefile/ChooseFile'
import EpubFile from './epubfile/EpubFile'
// 关于 exact https://stackoverflow.com/questions/49162311/react-difference-between-route-exact-path-and-route-path
export default class AppRouter extends Component {
    render() {
        return (
            // build后 且部署后 页面空白 https://github.com/gitname/react-gh-pages/issues/17
            <HashRouter basename='/client'>
                <Route exact path='/' component={App} />
                <Route path='/game' component={Game} />
                <Route path='/mobx' component={Mobx} />
                <Route path='/flyio' component={Flyio} />
                <Route path='/platform' component={Platform} />
                <Route path='/launchapp' component={LaunchApp} />
                <Route path='/choosefile' component={ChooseFile} />
                <Route path='/epubfile' component={EpubFile} />
            </HashRouter>
        )
    }
}