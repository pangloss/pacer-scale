require 'spec_helper'

describe 'simple rating scale' do
  before do
    @g = Pacer.tg
    @root = @g.create_vertex(PacerScale::Root)
    @root.generate_scale 'ranking', 1, 10, 1
  end

  it 'should find the first ranking' do
    v = @root.find 'ranking', 1
    v.should_not be_nil
  end

  it 'should find the last ranking' do
    v = @root.find 'ranking', 10
    v.should_not be_nil
  end
end
