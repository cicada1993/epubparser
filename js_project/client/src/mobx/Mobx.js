import React, { Component } from 'react'
import './mobx.css'
import { observer } from 'mobx-react'
import { trace } from 'mobx'
import DataSource from './DataSource'

// 对observable敏感的组件 作为下面组件Mobx的部分视图
const MutableComp = observer((props) => {
    console.log('MutableComp is rendering')
    const { dataSource } = props
    return (
        <div>{dataSource.content}</div>
    )
})

//官网文档 https://cn.mobx.js.org/
export default class Mobx extends Component {
    constructor(props) {
        super(props)
        this.dataSource = new DataSource()
        trace(this.dataSource, 'content')
    }
    render() {
        console.log('Mobx is rendering')
        return (
            <div className='mobx'>
                <div>this is mobx page</div>
                <MutableComp dataSource={this.dataSource} />
                <div><input onChange={(e) => { this.changeContent(e) }} placeholder="请输入文字修改上方内容" /></div>
            </div>

        )
    }

    // 让MutableComp中的内容随input标签内容变化
    changeContent(event) {
        this.dataSource.setContent(event.target.value)
    }
}