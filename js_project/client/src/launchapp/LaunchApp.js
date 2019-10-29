import React, { Component } from 'react'
import './launchapp.css'
import LaunchBridge from './LaunchBridge'
export default class LaunchApp extends Component {
    constructor(props) {
        super(props)
        window.launchBridge = new LaunchBridge()
    }
    render() {
        return (
            <div className='launchapp'>
                <div>this is page launchapp</div>
                <a onClick={() => { this.openApp() }} href='javascript:void()'>唤起微书房</a>
            </div>
        )
    }

    openApp() {
        //window.launchBridge.launchApp()
        console.log('ssss')
        window.location.href = 'microbook://localWeb/DownloadApp/'
    }

    componentWillUnmount() {
        console.log('componentWillUnmount')
        delete window.launchBridge
        this.sendCommand('退出页面')
    }
}