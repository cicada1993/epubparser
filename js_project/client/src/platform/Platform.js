import React, { Component } from 'react'
import './platform.css'
import JSBridge from './JSBridge'
// component生命周期 https://www.cnblogs.com/chunlei36/p/6415582.html
export default class Platform extends Component {
    constructor(props) {
        super(props)
        console.log('constructor')
        window.jsBridge = new JSBridge()
    }

    sendCommand(cmd) {
        if (window.platform) {
            window.platform.onCommand(cmd)
        }
    }

    componentWillMount() {
        console.log('componentWillMount')
    }

    render() {
        console.log('render')
        return (
            <div className='platform'>
                <div>this is page platform</div>
            </div>
        )
    }

    componentDidMount() {
        console.log('componentDidMount')
        this.sendCommand('进入页面')
    }

    componentWillReceiveProps(nextProps) {
        console.log('componentWillReceiveProps')
        super.componentWillReceiveProps(nextProps)
    }

    shouldComponentUpdate(nextProps, nextState) {
        console.log('shouldComponentUpdate')
        return super.shouldComponentUpdate(nextProps, nextState)
    }

    componentWillUpdate(nextProps, nextState) {
        console.log('componentWillUpdate')
        super.componentWillUpdate(nextProps, nextState)
    }

    componentDidUpdate(prevProps, prevState) {
        console.log('componentDidUpdate')
        super.componentDidUpdate(prevProps, prevState)
    }

    componentWillUnmount() {
        console.log('componentWillUnmount')
        delete window.jsBridge
        this.sendCommand('退出页面')
    }
}