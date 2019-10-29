import { LaunchApp as Launcher } from 'web-launch-app'
export default class LaunchBridge {
    launchApp() {
        console.log('launchApp')
        const launcher = new Launcher()
        launcher.open(
            {
                launchType: {
                    android: 'scheme'
                },
                scheme: 'microbook://localweb/DownloadApp',
                param: {
                    shId: '图书shId'
                },
                pkgs:{
                    android:'http://www.baidu.com'
                }
            },
            (s, d, url) => {
               
            }
        )
    }
}