import React, { Component } from 'react'
import './choosefile.css'
export default class ChooseFile extends Component {
    render() {
        return (
            <div>
                <div className='choosefile'>
                    <div>this is page choosefile</div>
                    <input placeholder={'请选择文件'} type = 'file'/>
                </div>
            </div>
        )
    }
}