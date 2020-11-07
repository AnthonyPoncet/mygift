import React from 'react';
import { RouteComponentProps } from "react-router";

import { history } from './history';

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();

interface PathParam { uuid: string };
interface StateProps extends RouteComponentProps<PathParam> { };
interface DispatchProps { };
type Props = DispatchProps & StateProps;
interface State { status: string, username: string, password: string, error: string };

class ResetPassword extends React.Component<Props, State> {
    private uuid: string;

    constructor(props: Props) {
        super(props);
        this.uuid = this.props.match.params.uuid;
        this.state = { status: "loading", username: "", password: "", error: "" }
    }

    _isUuidValid() {
        const request = async () => {
            const response = await fetch(url + '/passwords/reset/' + this.uuid, { method: 'get' });
            if (response.status === 302) {
                this.setState({ status: "valid" });
            } else {
                const json = await response.json();
                console.error(json);
                this.setState({ status: "invalid" });
            }
        };
        request();
    }

    componentDidMount() {
        this._isUuidValid()
    }

    _handleChange(e: any) {
        const { name, value } = e.target;
        if (name === "username") {
          this.setState({username: value, password: this.state.password});
        } else if (name === "password") {
          this.setState({username: this.state.username, password: value});
        }
    }

    _handleSubmit() {
        const { username, password } = this.state;
        const request = async () => {
            const response = await fetch(url + '/passwords/reset/' + this.uuid, {
                method: 'post',
                headers: {'Content-Type':'application/json'},
                body: JSON.stringify({ "name": username, "password": password })
            });
            if (response.status === 202) {
                this.setState({ status: "done" });
                history.push("/signin");
            } else {
                const json = await response.json();
                console.error(json);
                this.setState({ status: "failed", error: json.error });
            }
        };
        request();
    }

    render() {
        console.debug("Page for " + this.uuid)
        const { status, username, password } = this.state;

        if (status === "loading") {
            return (
                <div>
                    <p>Please wait</p>
                </div>
            )
        } else if (status === "valid") {
            return (
                <div className="auth-form">
                  <h1 className="auth-form-header">Reset password</h1>
                  <div className="auth-form-body">
                      <div className="form-group">
                        <label>Username</label>
                        <input type="text" name="username" placeholder="username" className="form-control" value={username} onChange={(e) => this._handleChange(e)} />
                      </div>
                      <div className="form-group">
                        <label>Password</label>
                        <input type="password" name="password" placeholder="password" className="form-control" value={password} onChange={(e) => this._handleChange(e)} />
                      </div>
                      <button className="btn btn-primary btn-large" onClick={() => this._handleSubmit()}>Change</button>
                  </div>
                </div>
            )
        } else if (status === "invalid") {
            return (<p>Page not valid</p>)
        } else if (status === "failed") {
            return(<p className="auth-error">{this.state.error}</p>)
        } else if (status === "done") {
            return(<p>Redirect...</p>)
        }
    }
}

export default ResetPassword