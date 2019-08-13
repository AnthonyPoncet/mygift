import React from 'react';

import { connect } from 'react-redux';
import { AppState } from '../redux/store';

import { HomeMessage } from '../translation/itrans';

interface Props { username: String | null, home: HomeMessage }

class HomePage extends React.Component<Props> {
  render() {
      return (
    <div>
        <h3>{this.props.home.hello} {this.props.username}</h3>
    </div>
  );
  }
}

function mapStateToProps(state: AppState) { return { username: state.signin.username, home: state.locale.messages.home }; }
export default connect(mapStateToProps)(HomePage);
