import React from 'react';

import { connect } from 'react-redux';

class HomePage extends React.Component {
  render() {
      return (
    <div>
        <h3>Hello {this.props.username}</h3>
    </div>
  );
  }
}

function mapStateToProps(state) {
    return { username: state.signin.username };
}
export default connect(mapStateToProps)(HomePage);
