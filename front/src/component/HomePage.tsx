import React from 'react';

import { connect } from 'react-redux';
import { AppState } from '../redux/store';

interface Props {
  username: String | null
}

class HomePage extends React.Component<Props> {
  render() {
      return (
    <div>
        <h3>Hello {this.props.username}</h3>
    </div>
  );
  }
}

function mapStateToProps(state: AppState) {
    return { username: state.signin.username };
}
export default connect(mapStateToProps)(HomePage);
