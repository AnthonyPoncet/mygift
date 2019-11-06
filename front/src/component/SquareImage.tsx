import React from 'react';

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();

interface Props { imageName: string | null, size: number, alt: string, alternateImage: any };
interface State { loadedUrl: any | null };

class SquareImage extends React.Component<Props, State> {
    constructor(props: Props) {
        super(props);
        this.state = { loadedUrl: null };

        const imageName = this.props.imageName === undefined ? null : this.props.imageName;
        if (imageName !== null) this._loadImage();
    }

    componentDidUpdate(prevProps : Props) {
      if (prevProps.imageName !== this.props.imageName) {
        const imageName = this.props.imageName === undefined ? null : this.props.imageName;
        if (imageName !== null) this._loadImage();
        this._loadImage();
      }
    }

    _loadImage() {
        const request = async() => {
            const response = await fetch(url + '/files/' + this.props.imageName);
            if (response.status === 404) {
                console.error("file " + this.props.imageName + " could not be found on server");
                return;
            }

            response.blob().then(blob => {
                let url = window.URL.createObjectURL(blob);
                this.setState({ loadedUrl: url });
            });
        };
        request();
    }

    render() {
        const { size, alt, alternateImage } = this.props;
        const { loadedUrl } = this.state;

        if (loadedUrl !== null){
            return <img height={size} width={size} src={loadedUrl} alt={alt}/>;
        } else {
            return <img height={size} width={size} src={alternateImage} alt="Nothing"/>;
        }
    }
}



export default SquareImage;