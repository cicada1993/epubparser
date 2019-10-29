import React, { Component } from 'react'
import './app.css'
import { Link } from 'react-router-dom'

export default class App extends Component {
  render() {
    return (
      <div className='app'>
        <div>this is home page</div>
        <div className='app-link'><Link to='/game'>to game</Link></div>
        <div className='app-link'><Link to='/mobx'>to mobx</Link></div>
        <div className='app-link'><Link to='/flyio'>to flyio</Link></div>
        <div className='app-link'><Link to='/platform'>to platform</Link></div>
        <div className='app-link'><Link to='/launchapp'>to launchapp</Link></div>
        <div className='app-link'><Link to='/choosefile'>to choosefile</Link></div>
        <div className='app-link'><Link to='/epubfile'>to epubfile</Link></div>
      </div>
    )
  }
}
