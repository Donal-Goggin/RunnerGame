package com.donal.runner.box2d;

import com.donal.runner.enums.UserDataType;

public class GroundUserData extends UserData {

    public GroundUserData() {
        super();
        userDataType = UserDataType.GROUND;
    }

    public GroundUserData(float width, float height) {
        super(width, height);
        userDataType = UserDataType.GROUND;
    }

}
