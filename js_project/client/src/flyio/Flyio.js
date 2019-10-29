import React, { Component } from 'react'
import fly from 'flyio'
import './flyio.css'
const HOST = "http://192.168.56.183:9595"
fly.config.baseURL = HOST
// 官网文档 https://wendux.github.io/dist/#/doc/flyio/readme
export default class Flyio extends Component {
    constructor(props) {
        super(props)
        this.state = {
            data: ''
        }
    }

    render() {
        return (
            <div className = 'flyio'>
                <div>this is page flyio</div>
                <button onClick={() => { this.request() }}>请求数据</button>
                <div>{this.state.data}</div>
            </div>
        )
    }

    request() {
        fly.get('/')
            .then((response) => {
                console.log(response)
                const {data} = response
                this.setState({
                    data: data
                })
            })  
            .catch((error)=>{
                console.log(error)
            })
    }
}